package ee.ut.quickbite.userservice.repository;

import ee.ut.quickbite.userservice.entity.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// DriverProfile shares its PK with User via @MapsId, so the id type is UUID.
public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {
    List<DriverProfile> findByIsAvailable(boolean isAvailable);
}
