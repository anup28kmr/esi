package ee.ut.quickbite.userservice.dto;

import java.util.UUID;

public record DriverProfileDTO(
        UUID userId,
        String vehicleType,
        String licenseNumber,
        boolean available) {
}
