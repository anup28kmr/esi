package ee.ut.anup.orderservice.service;

import ee.ut.anup.orderservice.dto.OrderItemResponse;
import ee.ut.anup.orderservice.dto.OrderResponse;
import ee.ut.anup.orderservice.dto.PlaceOrderRequest;
import ee.ut.anup.orderservice.dto.StatusUpdateRequest;

import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(Long customerId, PlaceOrderRequest request);
    OrderResponse getOrder(Long id);
    void cancelOrder(Long id);
    OrderResponse updateOrderStatus(Long id, StatusUpdateRequest request);
    List<OrderResponse> getOrdersByCustomer(Long customerId);
    List<OrderResponse> getOrdersByRestaurant(String restaurantId);
    List<OrderItemResponse> getOrderItems(Long id);
}
