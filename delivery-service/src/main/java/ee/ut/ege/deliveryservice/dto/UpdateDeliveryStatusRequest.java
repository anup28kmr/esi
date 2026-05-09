package ee.ut.ege.deliveryservice.dto;

import ee.ut.ege.deliveryservice.domain.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDeliveryStatusRequest {

    @NotNull(message = "status is required")
    private DeliveryStatus status;
}
