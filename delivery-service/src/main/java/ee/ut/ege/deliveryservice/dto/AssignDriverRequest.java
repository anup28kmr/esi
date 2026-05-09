package ee.ut.ege.deliveryservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignDriverRequest {

    @NotNull(message = "driverId is required")
    private UUID driverId;
}
