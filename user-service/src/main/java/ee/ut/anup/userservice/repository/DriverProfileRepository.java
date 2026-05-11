package ee.ut.anup.userservice.repository;

import ee.ut.anup.userservice.entity.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {
    List<DriverProfile> findByIsAvailable(boolean isAvailable);
}
