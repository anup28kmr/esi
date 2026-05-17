package ee.ut.ege.deliveryservice.repository;

import ee.ut.ege.deliveryservice.domain.Delivery;
import ee.ut.ege.deliveryservice.domain.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
    Optional<Delivery> findByOrderId(UUID orderId);
    boolean existsByOrderId(UUID orderId);
    List<Delivery> findByStatus(DeliveryStatus status);
    Optional<Delivery> findFirstByDriverIdAndStatusIn(UUID driverId, Collection<DeliveryStatus> statuses);
}
