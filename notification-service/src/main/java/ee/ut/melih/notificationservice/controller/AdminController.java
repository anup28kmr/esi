package ee.ut.melih.notificationservice.controller;

import ee.ut.melih.notificationservice.dto.EventEnvelope;
import ee.ut.melih.notificationservice.dto.PublishEventRequest;
import ee.ut.melih.notificationservice.service.KafkaEventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Admin", description = "Internal helpers for demo and integration tests.")
public class AdminController {

  private final KafkaEventPublisher publisher;

  public AdminController(KafkaEventPublisher publisher) {
    this.publisher = publisher;
  }

  @PostMapping("/publish-event")
  @Operation(
      summary = "Publish a test event to a Kafka topic",
      description =
          "Used to demonstrate the Notification Service's event-driven integration without "
              + "having to drive a full Payment/Delivery flow first.")
  public ResponseEntity<EventEnvelope> publish(@Valid @RequestBody PublishEventRequest request) {
    EventEnvelope event =
        EventEnvelope.of(request.type(), request.payload() == null ? Map.of() : request.payload());
    publisher.publish(request.topic(), event);
    return ResponseEntity.accepted().body(event);
  }
}
