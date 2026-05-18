package ee.ut.quickbite.orderservice.dto.external;

import java.util.UUID;

/**
 * Payload for delivery-service POST /deliveries. The receiving DTO is a
 * Lombok @Data bean (not a record), so Jackson matches by setter/property
 * name -- the field names here must line up exactly.
 */
public record CreateDeliveryRequest(
        UUID orderId,
        String pickupAddress,
        String deliveryAddress) {}
