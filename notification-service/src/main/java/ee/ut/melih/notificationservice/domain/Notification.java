package ee.ut.melih.notificationservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "recipient_id", nullable = false)
  private UUID recipientId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Channel channel;

  @Column(nullable = false, length = 1024)
  private String message;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private NotificationStatus status;

  protected Notification() {}

  public Notification(UUID recipientId, Channel channel, String message) {
    this.recipientId = recipientId;
    this.channel = channel;
    this.message = message;
    this.status = NotificationStatus.QUEUED;
  }

  public void markSent(Instant when) {
    this.status = NotificationStatus.SENT;
    this.sentAt = when;
  }

  public void markFailed() {
    this.status = NotificationStatus.FAILED;
  }

  public void markRead() {
    this.status = NotificationStatus.READ;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getRecipientId() {
    return recipientId;
  }

  public Channel getChannel() {
    return channel;
  }

  public String getMessage() {
    return message;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public NotificationStatus getStatus() {
    return status;
  }
}
