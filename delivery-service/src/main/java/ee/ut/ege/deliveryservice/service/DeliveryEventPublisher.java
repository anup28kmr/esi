package ee.ut.ege.deliveryservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ee.ut.ege.deliveryservice.config.KafkaTopics;
import ee.ut.ege.deliveryservice.domain.DeliveryStatus;
import ee.ut.ege.deliveryservice.dto.EventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class DeliveryEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DeliveryEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public void publishDeliveryCreated(UUID deliveryId, UUID orderId) {
        send("delivery.created", orderId, Map.of(
                "deliveryId", deliveryId.toString(),
                "orderId", orderId.toString()
        ));
    }

    public void publishDeliveryStatusUpdated(UUID deliveryId, UUID orderId, DeliveryStatus status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("deliveryId", deliveryId.toString());
        payload.put("orderId", orderId.toString());
        payload.put("status", status.name());
        send("delivery.status-changed", orderId, payload);
    }

    public void publishDeliveryStatusUpdated(UUID deliveryId, UUID orderId, DeliveryStatus status, UUID driverId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("deliveryId", deliveryId.toString());
        payload.put("orderId", orderId.toString());
        payload.put("status", status.name());
        payload.put("driverId", driverId.toString());
        send("delivery.status-changed", orderId, payload);
    }

    private void send(String type, UUID key, Map<String, Object> payload) {
        EventEnvelope event = EventEnvelope.of(type, payload);
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.DELIVERY_EVENTS, key.toString(), json);
            log.info("Published {} to {}", type, KafkaTopics.DELIVERY_EVENTS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event {}: {}", type, e.getMessage());
        }
    }
}
