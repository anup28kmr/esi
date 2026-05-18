package ee.ut.quickbite.orderservice.mapper;

import ee.ut.quickbite.orderservice.dto.OrderItemResponse;
import ee.ut.quickbite.orderservice.entity.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {

    public OrderItemResponse mapToResponse(OrderItem item) {
        if (item == null) {
            return null;
        }
        return new OrderItemResponse(
                item.getMenuItemId(),
                item.getName(),
                item.getUnitPrice(),
                item.getQuantity()
        );
    }
}
