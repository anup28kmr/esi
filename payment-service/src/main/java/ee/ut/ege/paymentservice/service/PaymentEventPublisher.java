package ee.ut.ege.paymentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stub for Kafka event publishing. Will produce to payment-events topic in Checkpoint 3.
 */
@Component
@Slf4j
public class PaymentEventPublisher {

    public void publishPaymentCompleted(UUID paymentId, UUID orderId, BigDecimal amount) {
        log.info("EVENT payment.completed: paymentId={}, orderId={}, amount={}", paymentId, orderId, amount);
    }

    public void publishPaymentFailed(UUID paymentId, UUID orderId, String reason) {
        log.info("EVENT payment.failed: paymentId={}, orderId={}, reason={}", paymentId, orderId, reason);
    }
}
