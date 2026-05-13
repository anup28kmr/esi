package ee.ut.melih.notificationservice.service;

import ee.ut.melih.notificationservice.config.KafkaTopics;
import ee.ut.melih.notificationservice.domain.Channel;
import ee.ut.melih.notificationservice.domain.ProcessedEvent;
import ee.ut.melih.notificationservice.dto.EventEnvelope;
import ee.ut.melih.notificationservice.dto.SendNotificationRequest;
import ee.ut.melih.notificationservice.repository.ProcessedEventRepository;
import java.util.Map;
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

  public KafkaEventConsumer(NotificationService notificationService,
                            ProcessedEventRepository processedEvents) {
    this.notificationService = notificationService;
    this.processedEvents = processedEvents;
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
      // Duplicate delivery (A3 §6.2 at-least-once). Acknowledge and move on.
      log.info("Skipping duplicate event {}", event.id());
      return;
    }
    UUID recipientId = resolveRecipient(event.payload());
    if (recipientId == null) {
      log.warn("Skipping event {} — no recipientId/customerId/userId/driverId in payload",
          event.id());
      return;
    }
    String message = (String) event.payload().getOrDefault("message", defaultMessage);
    notificationService.send(
        new SendNotificationRequest(recipientId, Channel.PUSH, message, event.type()));
  }

  /**
   * Inserts the event id into the idempotency table. Returns true if
   * this was the first time we saw the id; false if it was already
   * present (duplicate).
   */
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
          // fall through
        }
      }
    }
    return null;
  }
}
