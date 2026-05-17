package ee.ut.melih.notificationservice.service;

import ee.ut.melih.notificationservice.client.OrderClient;
import ee.ut.melih.notificationservice.config.KafkaTopics;
import ee.ut.melih.notificationservice.domain.Channel;
import ee.ut.melih.notificationservice.domain.ProcessedEvent;
import ee.ut.melih.notificationservice.dto.EventEnvelope;
import ee.ut.melih.notificationservice.dto.SendNotificationRequest;
import ee.ut.melih.notificationservice.repository.ProcessedEventRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

  private final NotificationService notificationService;
  private final ProcessedEventRepository processedEvents;
  private final OrderClient orderClient;

  public KafkaEventConsumer(NotificationService notificationService,
                            ProcessedEventRepository processedEvents,
                            OrderClient orderClient) {
    this.notificationService = notificationService;
    this.processedEvents = processedEvents;
    this.orderClient = orderClient;
  }

  // Human-readable fallback messages. Used only when the producer
  // does not put a `message` field in the payload, so the customer
  // never sees raw event-type strings like "delivery.created".
  private static final Map<String, String> FRIENDLY_DEFAULTS = Map.ofEntries(
      Map.entry("payment.completed",       "Your payment was confirmed"),
      Map.entry("payment.confirmed",       "Your payment was confirmed"),
      Map.entry("payment.failed",          "Your payment failed — please update your payment method"),
      Map.entry("payment.refunded",        "We refunded your order"),
      Map.entry("delivery.created",        "A courier was assigned to your order"),
      Map.entry("delivery.assigned",       "A courier was assigned to your order"),
      Map.entry("delivery.dispatched",     "Your order is on its way"),
      Map.entry("delivery.completed",      "Your order has been delivered"),
      Map.entry("order.placed",            "Your order was placed"),
      Map.entry("order.confirmed",         "Your order was confirmed"),
      Map.entry("order.cancelled",         "Your order was cancelled"));

  @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "notification-service")
  @Transactional
  public void onPaymentEvent(EventEnvelope event) {
    log.info("Received {} from {}", event.type(), KafkaTopics.PAYMENT_EVENTS);
    handle(event);
  }

  @KafkaListener(topics = KafkaTopics.DELIVERY_EVENTS, groupId = "notification-service")
  @Transactional
  public void onDeliveryEvent(EventEnvelope event) {
    log.info("Received {} from {}", event.type(), KafkaTopics.DELIVERY_EVENTS);
    handle(event);
  }

  @KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "notification-service")
  @Transactional
  public void onOrderEvent(EventEnvelope event) {
    log.info("Received {} from {}", event.type(), KafkaTopics.ORDER_EVENTS);
    handle(event);
  }

  private void handle(EventEnvelope event) {
    if (event.id() == null) {
      log.warn("Skipping event with null id (type={})", event.type());
      return;
    }
    if (!claim(event.id())) {
      log.info("Skipping duplicate event {}", event.id());
      return;
    }
    UUID recipientId = resolveRecipient(event.payload());
    if (recipientId == null) {
      // Fallback per A3 §6.2: the spec payload only carries domain ids
      // (paymentId, deliveryId, orderId). When the producer omits a
      // user id, look the customer up via Order Service.
      recipientId = lookupCustomerByOrder(event.payload()).orElse(null);
    }
    if (recipientId == null) {
      log.warn("Skipping event {} — no recipient could be resolved from payload or order lookup",
          event.id());
      return;
    }
    String message = (String) event.payload().getOrDefault("message", defaultMessageFor(event));
    notificationService.send(
        new SendNotificationRequest(recipientId, Channel.PUSH, message, event.type()));
  }

  /**
   * Picks a friendly fallback message when the producer does not
   * include one in the payload. {@code delivery.status-changed} is
   * special-cased because the meaning depends on the {@code status}
   * field (PickedUp, Delivered, ...).
   */
  private String defaultMessageFor(EventEnvelope event) {
    String type = event.type() == null ? "" : event.type().toLowerCase().replace('_', '.');
    if (type.equals("delivery.status-changed")
        || type.equals("delivery.status.changed")
        || type.equals("delivery.statuschanged")) {
      Object raw = event.payload() == null ? null : event.payload().get("status");
      String status = raw == null ? "" : raw.toString().toLowerCase().replaceAll("[_-]", "");
      if (status.contains("pickedup")) return "Your order has been picked up";
      if (status.contains("delivered")) return "Your order has been delivered";
      if (status.contains("intransit") || status.contains("dispatched"))
        return "Your order is on its way";
      return "Your delivery status was updated";
    }
    String mapped = FRIENDLY_DEFAULTS.get(type);
    if (mapped != null) return mapped;
    return "You have a new notification";
  }

  private boolean claim(UUID eventId) {
    if (processedEvents.existsById(eventId)) {
      return false;
    }
    try {
      processedEvents.save(new ProcessedEvent(eventId));
      return true;
    } catch (DataIntegrityViolationException collision) {
      return false;
    }
  }

  private UUID resolveRecipient(Map<String, Object> payload) {
    if (payload == null) return null;
    for (String key : new String[] {"recipientId", "customerId", "userId", "driverId"}) {
      Object value = payload.get(key);
      if (value != null) {
        try {
          return UUID.fromString(value.toString());
        } catch (IllegalArgumentException ignored) {
          // not a UUID — try the next key
        }
      }
    }
    return null;
  }

  private Optional<UUID> lookupCustomerByOrder(Map<String, Object> payload) {
    if (payload == null) return Optional.empty();
    Object raw = payload.get("orderId");
    if (raw == null) return Optional.empty();
    return parseOrderId(raw.toString())
        .flatMap(orderClient::getCustomerIdForOrder);
  }

  /**
   * Order Service uses {@code Long} order ids, but Payment / Delivery
   * services sometimes stringify them as zero-padded UUIDs (e.g.
   * {@code 00000000-0000-0000-0000-000000000001}). Accept either form
   * by trying plain Long first, then the UUID's trailing hex group.
   */
  private static Optional<Long> parseOrderId(String input) {
    if (input == null || input.isBlank()) return Optional.empty();
    try {
      return Optional.of(Long.parseLong(input.trim()));
    } catch (NumberFormatException ignored) {
      // fall through
    }
    String tail = input.contains("-") ? input.substring(input.lastIndexOf('-') + 1) : input;
    try {
      return Optional.of(Long.parseLong(tail, 16));
    } catch (NumberFormatException ignored) {
      return Optional.empty();
    }
  }
}
