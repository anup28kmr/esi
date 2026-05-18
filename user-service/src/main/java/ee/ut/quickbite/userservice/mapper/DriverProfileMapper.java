package ee.ut.quickbite.userservice.mapper;

import ee.ut.quickbite.userservice.dto.DriverProfileDTO;
import ee.ut.quickbite.userservice.entity.DriverProfile;
import org.springframework.stereotype.Component;

@Component
public class DriverProfileMapper {

    public DriverProfileDTO toDto(DriverProfile driverProfile) {
        return new DriverProfileDTO(
                driverProfile.getUserId(),
                driverProfile.getVehicleType(),
                driverProfile.getLicenseNumber(),
                driverProfile.isAvailable());
    }
}
