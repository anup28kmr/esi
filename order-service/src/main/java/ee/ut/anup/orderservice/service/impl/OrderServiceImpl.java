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
import ee.ut.anup.orderservice.entity.User;
import ee.ut.anup.orderservice.exception.ResourceNotFoundException;
import ee.ut.anup.orderservice.mapper.OrderItemMapper;
import ee.ut.anup.orderservice.mapper.OrderMapper;
import ee.ut.anup.orderservice.repository.OrderRepository;
import ee.ut.anup.orderservice.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserClient userClient;

    @Override
    @Transactional
    public OrderResponse placeOrder(Long customerId, PlaceOrderRequest request) {
        User user = userRepository.findById(customerId)
                .orElseGet(() -> {
                    UserDTO externalUser = userClient.getUserById(customerId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found in user-service with id: " + customerId));
                    User newUser = new User();
                    newUser.setUserId(externalUser.userId());
                    newUser.setEmail(externalUser.email());
                    newUser.setFullName(externalUser.fullName());
                    return userRepository.save(newUser);
                });

        Order order = new Order();
        order.setUser(user);
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
        return orderMapper.mapToResponse(savedOrder);
    }

    @Override
    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return orderMapper.mapToResponse(order);
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (!OrderServiceConstants.STATUS_PENDING.equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Only pending orders can be cancelled");
        }

        order.setStatus(OrderServiceConstants.STATUS_CANCELLED);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, StatusUpdateRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        order.setStatus(request.status().trim().toUpperCase());
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.mapToResponse(updatedOrder);
    }

    @Override
    public List<OrderResponse> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByUser_UserId(customerId).stream()
                .map(orderMapper::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getOrdersByRestaurant(String restaurantId) {
        return orderRepository.findByRestaurantId(restaurantId).stream()
                .map(orderMapper::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderItemResponse> getOrderItems(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        return order.getItems().stream()
                .map(orderItemMapper::mapToResponse)
                .collect(Collectors.toList());
    }

}
