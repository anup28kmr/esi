package ee.ut.melih.notificationservice.service;

import ee.ut.melih.notificationservice.config.KafkaTopics;
import ee.ut.melih.notificationservice.domain.Channel;
import ee.ut.melih.notificationservice.dto.EventEnvelope;
import ee.ut.melih.notificationservice.dto.SendNotificationRequest;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

  private final NotificationService notificationService;

  public KafkaEventConsumer(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "notification-service")
  public void onPaymentEvent(EventEnvelope event) {
    log.info("Received {} from {}", event.type(), KafkaTopics.PAYMENT_EVENTS);
    handle(event, "Payment update: " + event.type());
  }

  @KafkaListener(topics = KafkaTopics.DELIVERY_EVENTS, groupId = "notification-service")
  public void onDeliveryEvent(EventEnvelope event) {
    log.info("Received {} from {}", event.type(), KafkaTopics.DELIVERY_EVENTS);
    handle(event, "Delivery update: " + event.type());
  }

  @KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "notification-service")
  public void onOrderEvent(EventEnvelope event) {
    log.info("Received {} from {}", event.type(), KafkaTopics.ORDER_EVENTS);
    handle(event, "Order update: " + event.type());
  }

  private void handle(EventEnvelope event, String defaultMessage) {
    UUID recipientId = resolveRecipient(event.payload());
    if (recipientId == null) {
      log.warn("Skipping event {} — no recipientId/customerId in payload", event.id());
      return;
    }
    String message = (String) event.payload().getOrDefault("message", defaultMessage);
    notificationService.send(new SendNotificationRequest(recipientId, Channel.PUSH, message));
  }

  private UUID resolveRecipient(Map<String, Object> payload) {
    if (payload == null) return null;
    for (String key : new String[] {"recipientId", "customerId", "userId"}) {
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
