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

  @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "notification-service")
  @Transactional
  public void onPaymentEvent(EventEnvelope event) {
    log.info("Received {} from {}", event.type(), KafkaTopics.PAYMENT_EVENTS);
    handle(event, "Payment update: " + event.type());
  }

  @KafkaListener(topics = KafkaTopics.DELIVERY_EVENTS, groupId = "notification-service")
  @Transactional
  public void onDeliveryEvent(EventEnvelope event) {
    log.info("Received {} from {}", event.type(), KafkaTopics.DELIVERY_EVENTS);
    handle(event, "Delivery update: " + event.type());
  }

  @KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "notification-service")
  @Transactional
  public void onOrderEvent(EventEnvelope event) {
    log.info("Received {} from {}", event.type(), KafkaTopics.ORDER_EVENTS);
    handle(event, "Order update: " + event.type());
  }

  private void handle(EventEnvelope event, String defaultMessage) {
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
    String message = (String) event.payload().getOrDefault("message", defaultMessage);
    notificationService.send(
        new SendNotificationRequest(recipientId, Channel.PUSH, message, event.type()));
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
