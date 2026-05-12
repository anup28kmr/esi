package ee.ut.anup.orderservice.service.impl;

import ee.ut.anup.orderservice.client.UserClient;
import ee.ut.anup.orderservice.dto.OrderLineRequest;
import ee.ut.anup.orderservice.dto.OrderResponse;
import ee.ut.anup.orderservice.dto.PlaceOrderRequest;
import ee.ut.anup.orderservice.dto.StatusUpdateRequest;
import ee.ut.anup.orderservice.service.OrderService;
import ee.ut.anup.orderservice.constants.OrderServiceConstants;
import ee.ut.anup.orderservice.dto.OrderItemResponse;
import ee.ut.anup.orderservice.dto.external.UserDTO;
import ee.ut.anup.orderservice.entity.Order;
import ee.ut.anup.orderservice.entity.OrderItem;
import ee.ut.anup.orderservice.exception.ResourceNotFoundException;
import ee.ut.anup.orderservice.mapper.OrderItemMapper;
import ee.ut.anup.orderservice.mapper.OrderMapper;
import ee.ut.anup.orderservice.repository.OrderRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserClient userClient;

    @Override
    @Transactional
    public OrderResponse placeOrder(UUID customerId, PlaceOrderRequest request) {

        log.info("user customerId={} — fetching from user-service", customerId);
        UserDTO externalUser = userClient.getUserById(customerId)
                .orElseThrow(() -> {
                    log.warn("user-service lookup returned empty customerId={}", customerId);
                    return new ResourceNotFoundException("User not found in user-service with id: " + customerId);
                });


        Order order = new Order();
        order.setUserId(externalUser.userId());
        order.setRestaurantId(request.restaurantId());
        order.setStatus(OrderServiceConstants.STATUS_PENDING);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderLineRequest itemRequest : request.items()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setMenuItemId(itemRequest.menuItemId());
            item.setName(itemRequest.name());
            item.setUnitPrice(itemRequest.unitPrice());
            item.setQuantity(itemRequest.quantity());
            items.add(item);
            total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        order.setItems(items);
        order.setTotalAmount(total);

        Order savedOrder = orderRepository.save(order);
        log.info("order persisted orderId={} customerId={} restaurantId={} items={} total={} status={}",
                savedOrder.getOrderId(), customerId, request.restaurantId(),
                items.size(), total, OrderServiceConstants.STATUS_PENDING);
        return orderMapper.mapToResponse(savedOrder);
    }

    @Override
    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.debug("order lookup miss orderId={}", id);
                    return new ResourceNotFoundException("Order not found with id: " + id);
                });
        return orderMapper.mapToResponse(order);
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.debug("cancel target missing orderId={}", id);
                    return new ResourceNotFoundException("Order not found with id: " + id);
                });

        if (!OrderServiceConstants.STATUS_PENDING.equalsIgnoreCase(order.getStatus())) {
            log.warn("cancel rejected orderId={} currentStatus={} (only PENDING is cancellable)",
                    id, order.getStatus());
            throw new IllegalStateException("Only pending orders can be cancelled");
        }

        order.setStatus(OrderServiceConstants.STATUS_CANCELLED);
        orderRepository.save(order);
        log.info("order cancelled orderId={}", id);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, StatusUpdateRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.debug("status update target missing orderId={}", id);
                    return new ResourceNotFoundException("Order not found with id: " + id);
                });

        String previousStatus = order.getStatus();
        String newStatus = request.status().trim().toUpperCase();
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        log.info("order status transition orderId={} {} -> {}", id, previousStatus, newStatus);
        return orderMapper.mapToResponse(updatedOrder);
    }

    @Override
    public List<OrderResponse> getOrdersByCustomer(UUID customerId) {
        List<OrderResponse> orders = orderRepository.findByUserId(customerId).stream()
                .map(orderMapper::mapToResponse)
                .collect(Collectors.toList());
        log.debug("orders by customer customerId={} count={}", customerId, orders.size());
        return orders;
    }

    @Override
    public List<OrderResponse> getOrdersByRestaurant(String restaurantId) {
        List<OrderResponse> orders = orderRepository.findByRestaurantId(restaurantId).stream()
                .map(orderMapper::mapToResponse)
                .collect(Collectors.toList());
        log.debug("orders by restaurant restaurantId={} count={}", restaurantId, orders.size());
        return orders;
    }

    @Override
    public List<OrderItemResponse> getOrderItems(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.debug("items lookup target missing orderId={}", id);
                    return new ResourceNotFoundException("Order not found with id: " + id);
                });

        return order.getItems().stream()
                .map(orderItemMapper::mapToResponse)
                .collect(Collectors.toList());
    }

}
