package ee.ut.anup.orderservice.service.impl;

import ee.ut.anup.orderservice.client.DeliveryClient;
import ee.ut.anup.orderservice.client.MenuClient;
import ee.ut.anup.orderservice.client.PaymentClient;
import ee.ut.anup.orderservice.client.RestaurantClient;
import ee.ut.anup.orderservice.client.UserClient;
import ee.ut.anup.orderservice.constants.OrderServiceConstants;
import ee.ut.anup.orderservice.dto.OrderItemResponse;
import ee.ut.anup.orderservice.dto.OrderLineRequest;
import ee.ut.anup.orderservice.dto.OrderResponse;
import ee.ut.anup.orderservice.dto.PlaceOrderRequest;
import ee.ut.anup.orderservice.dto.StatusUpdateRequest;
import ee.ut.anup.orderservice.dto.external.AvailabilityResponse;
import ee.ut.anup.orderservice.dto.external.CreateDeliveryRequest;
import ee.ut.anup.orderservice.dto.external.CreatePaymentRequest;
import ee.ut.anup.orderservice.dto.external.DeliveryResponse;
import ee.ut.anup.orderservice.dto.external.PaymentResponse;
import ee.ut.anup.orderservice.dto.external.RestaurantSummaryResponse;
import ee.ut.anup.orderservice.dto.external.UserDTO;
import ee.ut.anup.orderservice.dto.external.ValidateMenuItemsRequest;
import ee.ut.anup.orderservice.dto.external.ValidateMenuItemsResponse;
import ee.ut.anup.orderservice.entity.Order;
import ee.ut.anup.orderservice.entity.OrderItem;
import ee.ut.anup.orderservice.exception.ResourceNotFoundException;
import ee.ut.anup.orderservice.mapper.OrderItemMapper;
import ee.ut.anup.orderservice.mapper.OrderMapper;
import ee.ut.anup.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// stubHappyPath() over-stubs deliberately so each failure-path test can
// override one collaborator; LENIENT prevents Mockito from rejecting the
// unused stubs.
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RESTAURANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MENU_ITEM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PAYMENT_ORDER_ID = new UUID(0L, 1L);

    @Mock private OrderRepository orderRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private UserClient userClient;
    @Mock private RestaurantClient restaurantClient;
    @Mock private MenuClient menuClient;
    @Mock private PaymentClient paymentClient;
    @Mock private DeliveryClient deliveryClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderResponse orderResponse;
    private List<String> statusTimeline;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setOrderId(1L);
        order.setUserId(CUSTOMER_ID);
        order.setRestaurantId(RESTAURANT_ID.toString());
        order.setStatus(OrderServiceConstants.STATUS_PLACED);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setItems(Collections.emptyList());

        orderResponse = new OrderResponse(1L, CUSTOMER_ID, RESTAURANT_ID.toString(),
                OrderServiceConstants.STATUS_CONFIRMED, new BigDecimal("100.00"),
                Collections.emptyList(), true, true);

        statusTimeline = new ArrayList<>();
    }

    /**
     * Snapshot each save() call so we can assert the status transition
     * timeline without falling into Mockito's mutable-argument trap (the
     * service mutates `order.status` between save calls; ArgumentCaptor
     * stores only the final state).
     */
    private Order copyForSave(Order src) {
        Order copy = new Order();
        copy.setOrderId(src.getOrderId() == null ? 1L : src.getOrderId());
        copy.setUserId(src.getUserId());
        copy.setRestaurantId(src.getRestaurantId());
        copy.setStatus(src.getStatus());
        copy.setTotalAmount(src.getTotalAmount());
        copy.setItems(src.getItems());
        return copy;
    }

    private PlaceOrderRequest sampleRequest() {
        return new PlaceOrderRequest(
                RESTAURANT_ID.toString(),
                List.of(new OrderLineRequest(MENU_ITEM_ID.toString(), "Item 1",
                        new BigDecimal("999.00"), 2)),  // client price is intentionally bogus
                "Narva mnt 1, Tallinn");
    }

    private void stubHappyPath() {
        UserDTO externalUser = new UserDTO(CUSTOMER_ID, "test@example.com", "Test User");
        when(userClient.getUserById(CUSTOMER_ID)).thenReturn(Optional.of(externalUser));

        when(restaurantClient.checkAvailability(RESTAURANT_ID)).thenReturn(Optional.of(
                new AvailabilityResponse(RESTAURANT_ID, true, true, "10:00-22:00", Instant.now())));
        when(restaurantClient.getRestaurant(RESTAURANT_ID)).thenReturn(Optional.of(
                new RestaurantSummaryResponse(RESTAURANT_ID, UUID.randomUUID(), "Diner", "Tartu mnt 5", "Tallinn")));

        ValidateMenuItemsResponse menuOk = new ValidateMenuItemsResponse(
                true,
                List.of(new ValidateMenuItemsResponse.Line(
                        MENU_ITEM_ID, 2, true, true,
                        new BigDecimal("50.00"), "EUR", new BigDecimal("100.00"), null)),
                new BigDecimal("100.00"), "EUR");
        when(menuClient.validate(any(ValidateMenuItemsRequest.class))).thenReturn(Optional.of(menuOk));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order src = inv.getArgument(0);
            statusTimeline.add(src.getStatus());
            // Return a fresh copy so subsequent service mutations don't
            // overwrite earlier snapshots captured by ArgumentCaptor.
            return copyForSave(src);
        });
        when(orderMapper.mapToResponse(any(Order.class))).thenReturn(orderResponse);

        when(paymentClient.createPayment(any(CreatePaymentRequest.class))).thenReturn(Optional.of(
                new PaymentResponse(UUID.randomUUID(), PAYMENT_ORDER_ID, new BigDecimal("100.00"),
                        "EUR", "COMPLETED", Instant.now())));
        when(deliveryClient.createDelivery(any(CreateDeliveryRequest.class))).thenReturn(Optional.of(
                new DeliveryResponse(UUID.randomUUID(), PAYMENT_ORDER_ID, "PENDING")));
    }

    @Test
    void placeOrder_happyPath_persistsThreeStatusTransitionsAndReturnsResponse() {
        stubHappyPath();

        OrderResponse response = orderService.placeOrder(CUSTOMER_ID, sampleRequest());

        assertNotNull(response);
        assertEquals(orderResponse.orderId(), response.orderId());

        verify(orderRepository, times(3)).save(any(Order.class));
        assertEquals(List.of(
                        OrderServiceConstants.STATUS_PLACED,
                        OrderServiceConstants.STATUS_PAID,
                        OrderServiceConstants.STATUS_CONFIRMED),
                statusTimeline);

        verify(userClient).getUserById(CUSTOMER_ID);
        verify(restaurantClient).checkAvailability(RESTAURANT_ID);
        verify(menuClient).validate(any(ValidateMenuItemsRequest.class));
        verify(paymentClient).createPayment(any(CreatePaymentRequest.class));
        verify(deliveryClient).createDelivery(any(CreateDeliveryRequest.class));
    }

    @Test
    void placeOrder_ignoresClientUnitPriceAndUsesMenuServicePrice() {
        stubHappyPath();

        orderService.placeOrder(CUSTOMER_ID, sampleRequest());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(3)).save(captor.capture());
        // The first save captures the production-side Order built inside
        // placeOrder() -- subsequent saves operate on the copy our answer
        // returned, so this assertion is safe.
        Order placed = captor.getAllValues().get(0);
        // Total = 2 * 50.00 (menu-service) = 100.00 -- NOT 2 * 999.00 (client claim).
        assertEquals(new BigDecimal("100.00"), placed.getTotalAmount());
        assertEquals(new BigDecimal("50.00"), placed.getItems().get(0).getUnitPrice());
    }

    @Test
    void placeOrder_sendsDerivedUuidAndTotalToPaymentService() {
        stubHappyPath();

        orderService.placeOrder(CUSTOMER_ID, sampleRequest());

        ArgumentCaptor<CreatePaymentRequest> captor = ArgumentCaptor.forClass(CreatePaymentRequest.class);
        verify(paymentClient).createPayment(captor.capture());
        CreatePaymentRequest sent = captor.getValue();
        assertEquals(PAYMENT_ORDER_ID, sent.orderId());
        assertEquals(new BigDecimal("100.00"), sent.amount());
        assertEquals("EUR", sent.currency());
    }

    @Test
    void placeOrder_sendsPickupAndDropoffToDeliveryService() {
        stubHappyPath();

        orderService.placeOrder(CUSTOMER_ID, sampleRequest());

        ArgumentCaptor<CreateDeliveryRequest> captor = ArgumentCaptor.forClass(CreateDeliveryRequest.class);
        verify(deliveryClient).createDelivery(captor.capture());
        CreateDeliveryRequest sent = captor.getValue();
        assertEquals(PAYMENT_ORDER_ID, sent.orderId());
        assertEquals("Tartu mnt 5, Tallinn", sent.pickupAddress());
        assertEquals("Narva mnt 1, Tallinn", sent.deliveryAddress());
    }

    @Test
    void placeOrder_failsWhenUserMissing() {
        when(userClient.getUserById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.placeOrder(CUSTOMER_ID, sampleRequest()));

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentClient, never()).createPayment(any());
        verify(deliveryClient, never()).createDelivery(any());
    }

    @Test
    void placeOrder_failsWhenRestaurantClosed() {
        when(userClient.getUserById(CUSTOMER_ID)).thenReturn(Optional.of(
                new UserDTO(CUSTOMER_ID, "x", "y")));
        when(restaurantClient.checkAvailability(RESTAURANT_ID)).thenReturn(Optional.of(
                new AvailabilityResponse(RESTAURANT_ID, false, false, "10:00-22:00", Instant.now())));

        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(CUSTOMER_ID, sampleRequest()));

        verify(menuClient, never()).validate(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_failsWhenMenuItemInvalid() {
        when(userClient.getUserById(CUSTOMER_ID)).thenReturn(Optional.of(
                new UserDTO(CUSTOMER_ID, "x", "y")));
        when(restaurantClient.checkAvailability(RESTAURANT_ID)).thenReturn(Optional.of(
                new AvailabilityResponse(RESTAURANT_ID, true, true, null, Instant.now())));
        when(menuClient.validate(any())).thenReturn(Optional.of(new ValidateMenuItemsResponse(
                false,
                List.of(new ValidateMenuItemsResponse.Line(
                        MENU_ITEM_ID, 2, false, false, null, null, null,
                        ValidateMenuItemsResponse.class.getSimpleName())),
                BigDecimal.ZERO, "EUR")));

        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(CUSTOMER_ID, sampleRequest()));

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentClient, never()).createPayment(any());
    }

    @Test
    void placeOrder_failsWhenPaymentNotCompleted() {
        stubHappyPath();
        when(paymentClient.createPayment(any())).thenReturn(Optional.of(
                new PaymentResponse(UUID.randomUUID(), PAYMENT_ORDER_ID,
                        new BigDecimal("100.00"), "EUR", "FAILED", Instant.now())));

        assertThrows(IllegalStateException.class,
                () -> orderService.placeOrder(CUSTOMER_ID, sampleRequest()));

        // Order was persisted as PLACED but never advanced to PAID/CONFIRMED.
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(deliveryClient, never()).createDelivery(any());
    }

    @Test
    void placeOrder_failsWhenDeliveryServiceDoesNotRespond() {
        stubHappyPath();
        when(deliveryClient.createDelivery(any())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> orderService.placeOrder(CUSTOMER_ID, sampleRequest()));

        // PLACED + PAID saved; CONFIRMED never reached.
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void placeOrder_rejectsEmptyItems() {
        PlaceOrderRequest empty = new PlaceOrderRequest(
                RESTAURANT_ID.toString(), Collections.emptyList(), "addr");

        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(CUSTOMER_ID, empty));
        verify(userClient, never()).getUserById(any());
    }

    @Test
    void placeOrder_rejectsBlankDeliveryAddress() {
        PlaceOrderRequest blank = new PlaceOrderRequest(
                RESTAURANT_ID.toString(),
                List.of(new OrderLineRequest(MENU_ITEM_ID.toString(), "x",
                        new BigDecimal("1"), 1)),
                "   ");

        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(CUSTOMER_ID, blank));
    }

    @Test
    void placeOrder_rejectsNonUuidRestaurantId() {
        PlaceOrderRequest bad = new PlaceOrderRequest(
                "not-a-uuid",
                List.of(new OrderLineRequest(MENU_ITEM_ID.toString(), "x",
                        new BigDecimal("1"), 1)),
                "addr");

        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(CUSTOMER_ID, bad));
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
        order.setStatus(OrderServiceConstants.STATUS_CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(1L));
    }

    @Test
    void updateOrderStatus_Success() {
        StatusUpdateRequest request = new StatusUpdateRequest("ACCEPTED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.mapToResponse(any(Order.class)))
                .thenReturn(orderResponse.withStatus("ACCEPTED"));

        OrderResponse response = orderService.updateOrderStatus(1L, request);

        assertNotNull(response);
        assertEquals("ACCEPTED", response.status());
    }

    // ---- accept / reject ---------------------------------------------------

    private static final UUID OWNER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_OWNER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private void stubConfirmedOrderOwnedBy(UUID ownerId) {
        order.setStatus(OrderServiceConstants.STATUS_CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(restaurantClient.getRestaurant(RESTAURANT_ID)).thenReturn(Optional.of(
                new RestaurantSummaryResponse(RESTAURANT_ID, ownerId, "Diner", "X", "Y")));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.mapToResponse(any(Order.class)))
                .thenAnswer(inv -> orderResponse.withStatus(((Order) inv.getArgument(0)).getStatus()));
    }

    @Test
    void acceptOrder_transitionsConfirmedToAccepted() {
        stubConfirmedOrderOwnedBy(OWNER_ID);

        OrderResponse response = orderService.acceptOrder(1L, OWNER_ID);

        assertEquals(OrderServiceConstants.STATUS_ACCEPTED, response.status());
        assertEquals(OrderServiceConstants.STATUS_ACCEPTED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void rejectOrder_transitionsConfirmedToRejected() {
        stubConfirmedOrderOwnedBy(OWNER_ID);

        OrderResponse response = orderService.rejectOrder(1L, OWNER_ID);

        assertEquals(OrderServiceConstants.STATUS_REJECTED, response.status());
        assertEquals(OrderServiceConstants.STATUS_REJECTED, order.getStatus());
    }

    @Test
    void acceptOrder_rejectsNonOwner() {
        stubConfirmedOrderOwnedBy(OWNER_ID);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> orderService.acceptOrder(1L, OTHER_OWNER_ID));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void acceptOrder_failsWhenRestaurantLookupEmpty() {
        order.setStatus(OrderServiceConstants.STATUS_CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(restaurantClient.getRestaurant(RESTAURANT_ID)).thenReturn(Optional.empty());

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> orderService.acceptOrder(1L, OWNER_ID));
    }

    @Test
    void acceptOrder_rejectsWrongStatus() {
        order.setStatus(OrderServiceConstants.STATUS_PLACED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.acceptOrder(1L, OWNER_ID));
        verify(restaurantClient, never()).getRestaurant(any());
    }

    @Test
    void rejectOrder_rejectsWrongStatus() {
        order.setStatus(OrderServiceConstants.STATUS_ACCEPTED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.rejectOrder(1L, OWNER_ID));
    }

    @Test
    void acceptOrder_orderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.acceptOrder(99L, OWNER_ID));
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
        item.setMenuItemId(MENU_ITEM_ID.toString());
        item.setName("Item 1");
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setQuantity(2);
        order.setItems(List.of(item));

        OrderItemResponse itemResponse = new OrderItemResponse(MENU_ITEM_ID.toString(),
                "Item 1", new BigDecimal("10.00"), 2);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemMapper.mapToResponse(item)).thenReturn(itemResponse);

        List<OrderItemResponse> responses = orderService.getOrderItems(1L);

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals(MENU_ITEM_ID.toString(), responses.get(0).menuItemId());
    }
}
