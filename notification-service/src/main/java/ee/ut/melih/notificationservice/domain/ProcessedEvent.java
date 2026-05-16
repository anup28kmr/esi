package ee.ut.melih.notificationservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Idempotency marker for Kafka events. A3 §6.2 requires consumers to
 * be idempotent (the event id is the key, a duplicate event must be
 * handled only once). We insert one row per processed event id; a
 * second arrival of the same id collides on the PK and is skipped.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

  @Id
  @Column(name = "event_id")
  private UUID eventId;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  protected ProcessedEvent() {}

  public ProcessedEvent(UUID eventId) {
    this.eventId = eventId;
    this.processedAt = Instant.now();
  }

  public UUID getEventId() {
    return eventId;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }
}
