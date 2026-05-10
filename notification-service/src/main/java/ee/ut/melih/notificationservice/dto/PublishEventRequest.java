package ee.ut.melih.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record PublishEventRequest(
    @NotBlank String topic, @NotBlank String type, Map<String, Object> payload) {}
