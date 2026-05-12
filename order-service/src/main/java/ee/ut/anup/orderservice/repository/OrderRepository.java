package ee.ut.anup.orderservice.repository;

import ee.ut.anup.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(UUID userId);

    List<Order> findByRestaurantId(String restaurantId);
}
