package ee.ut.quickbite.orderservice.service;

import ee.ut.quickbite.orderservice.dto.OrderItemResponse;
import ee.ut.quickbite.orderservice.dto.OrderResponse;
import ee.ut.quickbite.orderservice.dto.PlaceOrderRequest;
import ee.ut.quickbite.orderservice.dto.StatusUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse placeOrder(UUID customerId, PlaceOrderRequest request);
    OrderResponse getOrder(Long id);
    void cancelOrder(Long id);
    OrderResponse updateOrderStatus(Long id, StatusUpdateRequest request);
    OrderResponse acceptOrder(Long id, UUID actorUserId);
    OrderResponse rejectOrder(Long id, UUID actorUserId);
    List<OrderResponse> getOrdersByCustomer(UUID customerId);
    List<OrderResponse> getOrdersByRestaurant(String restaurantId);
    List<OrderItemResponse> getOrderItems(Long id);
}
