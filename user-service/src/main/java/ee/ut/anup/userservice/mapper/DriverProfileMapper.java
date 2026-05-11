package ee.ut.anup.userservice.mapper;

import ee.ut.anup.userservice.dto.DriverProfileDTO;
import ee.ut.anup.userservice.entity.DriverProfile;
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
