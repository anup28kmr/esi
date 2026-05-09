package ee.ut.ege.deliveryservice.dto;

import ee.ut.ege.deliveryservice.domain.DeliveryStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DeliveryResponse {
    private UUID deliveryId;
    private UUID orderId;
    private UUID driverId;
    private DeliveryStatus status;
    private String pickupAddress;
    private String deliveryAddress;
    private Instant estimatedDeliveryTime;
    private Instant actualDeliveryTime;
    private Instant createdAt;
    private Instant updatedAt;
}
