package ee.ut.anup.orderservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
    Long orderId,
    Long customerId,
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
