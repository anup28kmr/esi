package ee.ut.quickbite.orderservice.dto;

import java.util.List;

public record PlaceOrderRequest(
        String restaurantId,
        List<OrderLineRequest> items,
        String deliveryAddress) {}
