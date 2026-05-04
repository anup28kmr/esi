package ee.ut.anup.userservice.dto;

public record DriverProfileDTO(
        Long userId,
        String vehicleType,
        String licenseNumber,
        boolean available) {
}
