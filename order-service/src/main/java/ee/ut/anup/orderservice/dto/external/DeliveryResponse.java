package ee.ut.anup.orderservice.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Consumer-side mirror of delivery-service DeliveryResponse. We only read the
 * id and the status returned by /deliveries; ignore the rest.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryResponse(
        UUID deliveryId,
        UUID orderId,
        String status) {}
