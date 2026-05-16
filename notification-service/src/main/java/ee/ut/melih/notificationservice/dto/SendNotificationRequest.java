package ee.ut.melih.notificationservice.dto;

import ee.ut.melih.notificationservice.domain.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SendNotificationRequest(
    @NotNull UUID recipientId,
    @NotNull Channel channel,
    @NotBlank @Size(max = 1024) String message,
    @Size(max = 64) String eventType) {

  public SendNotificationRequest(UUID recipientId, Channel channel, String message) {
    this(recipientId, channel, message, null);
  }
}
