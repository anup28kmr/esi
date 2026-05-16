package ee.ut.anup.orderservice.dto;

import java.util.List;

public record PlaceOrderRequest(
        String restaurantId,
        List<OrderLineRequest> items,
        String deliveryAddress) {}
