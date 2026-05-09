package ee.ut.ege.deliveryservice.service;

import ee.ut.ege.deliveryservice.domain.DeliveryStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub for Kafka event publishing. Will produce to delivery-events topic in Checkpoint 3.
 */
@Component
@Slf4j
public class DeliveryEventPublisher {

    public void publishDeliveryCreated(UUID deliveryId, UUID orderId) {
        log.info("EVENT delivery.created: deliveryId={}, orderId={}", deliveryId, orderId);
    }

    public void publishDeliveryStatusUpdated(UUID deliveryId, UUID orderId, DeliveryStatus status) {
        log.info("EVENT delivery.statusUpdated: deliveryId={}, orderId={}, status={}", deliveryId, orderId, status);
    }
}
