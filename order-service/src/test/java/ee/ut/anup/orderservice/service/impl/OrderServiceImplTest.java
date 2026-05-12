package ee.ut.anup.orderservice.service.impl;

import ee.ut.anup.orderservice.client.UserClient;
import ee.ut.anup.orderservice.constants.OrderServiceConstants;
import ee.ut.anup.orderservice.dto.*;
import ee.ut.anup.orderservice.dto.external.UserDTO;
import ee.ut.anup.orderservice.entity.Order;
import ee.ut.anup.orderservice.entity.OrderItem;
import ee.ut.anup.orderservice.exception.ResourceNotFoundException;
import ee.ut.anup.orderservice.mapper.OrderItemMapper;
import ee.ut.anup.orderservice.mapper.OrderMapper;
import ee.ut.anup.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setOrderId(1L);
        order.setUserId(CUSTOMER_ID);
        order.setRestaurantId("rest1");
        order.setStatus(OrderServiceConstants.STATUS_PENDING);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setItems(Collections.emptyList());

        orderResponse = new OrderResponse(1L, CUSTOMER_ID, "rest1", "PENDING",
                new BigDecimal("100.00"), Collections.emptyList(), true, true);
    }

    @Test
    void placeOrder_Success() {
        PlaceOrderRequest request = new PlaceOrderRequest("rest1",
            List.of(new OrderLineRequest("item1", "Item 1", new BigDecimal("50.00"), 2)));
        UserDTO externalUser = new UserDTO(CUSTOMER_ID, "test@example.com", "Test User");

        when(userClient.getUserById(CUSTOMER_ID)).thenReturn(Optional.of(externalUser));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.mapToResponse(any(Order.class))).thenReturn(orderResponse);

        OrderResponse response = orderService.placeOrder(CUSTOMER_ID, request);

        assertNotNull(response);
        assertEquals(orderResponse.orderId(), response.orderId());
        verify(userClient).getUserById(CUSTOMER_ID);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void placeOrder_UserNotFoundInExternalService() {
        PlaceOrderRequest request = new PlaceOrderRequest("rest1", Collections.emptyList());

        when(userClient.getUserById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.placeOrder(CUSTOMER_ID, request));
        verify(userClient).getUserById(CUSTOMER_ID);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrder_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.mapToResponse(order)).thenReturn(orderResponse);

        OrderResponse response = orderService.getOrder(1L);

        assertNotNull(response);
        assertEquals(1L, response.orderId());
    }

    @Test
    void getOrder_NotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrder(1L));
    }

    @Test
    void cancelOrder_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.cancelOrder(1L);

        assertEquals(OrderServiceConstants.STATUS_CANCELLED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_IllegalState() {
        order.setStatus(OrderServiceConstants.STATUS_ACCEPTED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(1L));
    }

    @Test
    void updateOrderStatus_Success() {
        StatusUpdateRequest request = new StatusUpdateRequest("ACCEPTED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.mapToResponse(any(Order.class))).thenReturn(orderResponse.withStatus("ACCEPTED"));

        OrderResponse response = orderService.updateOrderStatus(1L, request);

        assertNotNull(response);
        assertEquals("ACCEPTED", response.status());
    }

    @Test
    void getOrdersByCustomer_Success() {
        when(orderRepository.findByUserId(CUSTOMER_ID)).thenReturn(List.of(order));
        when(orderMapper.mapToResponse(order)).thenReturn(orderResponse);

        List<OrderResponse> responses = orderService.getOrdersByCustomer(CUSTOMER_ID);

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
    }

    @Test
    void getOrderItems_Success() {
        OrderItem item = new OrderItem();
        item.setMenuItemId("m1");
        item.setName("Item 1");
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setQuantity(2);
        order.setItems(List.of(item));

        OrderItemResponse itemResponse = new OrderItemResponse("m1", "Item 1", new BigDecimal("10.00"), 2);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemMapper.mapToResponse(item)).thenReturn(itemResponse);

        List<OrderItemResponse> responses = orderService.getOrderItems(1L);

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals("m1", responses.get(0).menuItemId());
    }
}
