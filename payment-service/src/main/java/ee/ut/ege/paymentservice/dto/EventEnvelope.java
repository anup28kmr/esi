package ee.ut.ege.paymentservice.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EventEnvelope(
        UUID id,
        String type,
        Instant occurredAt,
        Map<String, Object> payload) {

    public static EventEnvelope of(String type, Map<String, Object> payload) {
        return new EventEnvelope(UUID.randomUUID(), type, Instant.now(), payload);
    }
}
