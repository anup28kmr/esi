package ee.ut.anup.orderservice.mapper;

import ee.ut.anup.orderservice.dto.OrderItemResponse;
import ee.ut.anup.orderservice.dto.OrderResponse;
import ee.ut.anup.orderservice.entity.Order;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class OrderMapper {

    private final OrderItemMapper orderItemMapper;

    public OrderResponse mapToResponse(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(orderItemMapper::mapToResponse)
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getOrderId(),
                order.getUserId(),
                order.getRestaurantId(),
                order.getStatus(),
                order.getTotalAmount(),
                itemResponses,
                true, // Mocking paymentStarted as in original code
                true  // Mocking deliveryTaskCreated as in original code
        );
    }
}
