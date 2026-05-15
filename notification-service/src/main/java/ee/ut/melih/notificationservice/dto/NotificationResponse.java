package ee.ut.melih.notificationservice.dto;

import ee.ut.melih.notificationservice.domain.Channel;
import ee.ut.melih.notificationservice.domain.Notification;
import ee.ut.melih.notificationservice.domain.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID recipientId,
    Channel channel,
    String message,
    String eventType,
    Instant sentAt,
    NotificationStatus status) {

  public static NotificationResponse from(Notification entity) {
    return new NotificationResponse(
        entity.getId(),
        entity.getRecipientId(),
        entity.getChannel(),
        entity.getMessage(),
        entity.getEventType(),
        entity.getSentAt(),
        entity.getStatus());
  }
}
