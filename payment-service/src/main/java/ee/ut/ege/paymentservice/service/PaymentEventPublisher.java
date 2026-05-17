package ee.ut.ege.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ee.ut.ege.paymentservice.config.KafkaProducerConfig;
import ee.ut.ege.paymentservice.dto.EventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public void publishPaymentCompleted(UUID paymentId, UUID orderId, BigDecimal amount) {
        send("payment.completed", orderId, Map.of(
                "paymentId", paymentId.toString(),
                "orderId", orderId.toString(),
                "amount", amount.toPlainString()
        ));
    }

    public void publishPaymentFailed(UUID paymentId, UUID orderId, String reason) {
        send("payment.failed", orderId, Map.of(
                "paymentId", paymentId.toString(),
                "orderId", orderId.toString(),
                "reason", reason
        ));
    }

    private void send(String type, UUID key, Map<String, Object> payload) {
        EventEnvelope event = EventEnvelope.of(type, payload);
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaProducerConfig.PAYMENT_EVENTS, key.toString(), json);
            log.info("Published {} to {}", type, KafkaProducerConfig.PAYMENT_EVENTS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event {}: {}", type, e.getMessage());
        }
    }
}
