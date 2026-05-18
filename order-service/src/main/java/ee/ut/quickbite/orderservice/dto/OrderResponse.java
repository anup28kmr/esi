package ee.ut.quickbite.orderservice.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    Long orderId,
    UUID customerId,
    String restaurantId,
    String status,
    BigDecimal totalAmount,
    List<OrderItemResponse> items,
    boolean paymentStarted,
    boolean deliveryTaskCreated) {
  public OrderResponse withStatus(String newStatus) {
    return new OrderResponse(
        orderId,
        customerId,
        restaurantId,
        newStatus,
        totalAmount,
        items,
        paymentStarted,
        deliveryTaskCreated);
  }
}
