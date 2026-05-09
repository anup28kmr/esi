package ee.ut.ege.deliveryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CreateDeliveryRequest {

    @NotNull(message = "orderId is required")
    private UUID orderId;

    @NotBlank(message = "pickupAddress is required")
    private String pickupAddress;

    @NotBlank(message = "deliveryAddress is required")
    private String deliveryAddress;

    private Instant estimatedDeliveryTime;
}
