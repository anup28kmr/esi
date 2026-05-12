package ee.ut.anup.userservice.repository;

import ee.ut.anup.userservice.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, Long> {
    // Address PK stays Long (its own row id); the FK column user_id is UUID
    // because Address.user references User which now has UUID PK.
    List<Address> findByUserUserId(UUID userId);
}
