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
import ee.ut.anup.orderservice.service.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final RestaurantClient restaurantClient;
    private final MenuClient menuClient;
    private final PaymentClient paymentClient;
    private final DeliveryClient deliveryClient;

    /**
     * Orchestrates the full place-order flow per the spec:
     * <ol>
     *   <li>Confirm the customer is active (user-service).</li>
     *   <li>Confirm the restaurant is open and accepting orders.</li>
     *   <li>Validate the items and pull authoritative unit prices from
     *       menu-service. The client-supplied unitPrice is ignored.</li>
     *   <li>Save the order with status = PLACED.</li>
     *   <li>Charge synchronously via payment-service; on COMPLETED,
     *       advance to PAID.</li>
     *   <li>Schedule delivery via delivery-service; on success,
     *       advance to CONFIRMED.</li>
     * </ol>
     * <p>Intentionally not method-level @Transactional: each repository save
     * commits in its own transaction so the status timeline (PLACED -> PAID
     * -> CONFIRMED) is durable. If a later step fails the order stays in the
     * last successfully persisted state -- recoverable by operator action or
     * the cancel endpoint -- instead of vanishing along with the customer's
     * already-completed payment.
     */
    @Override
    public OrderResponse placeOrder(UUID customerId, PlaceOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        if (request.deliveryAddress() == null || request.deliveryAddress().isBlank()) {
            throw new IllegalArgumentException("deliveryAddress is required");
        }
        UUID restaurantUuid = parseRestaurantId(request.restaurantId());

        // 1. user-service: confirm customer exists and is ACTIVE.
        log.info("placing order customerId={} restaurantId={} lines={}",
                customerId, restaurantUuid, request.items().size());
        UserDTO externalUser = userClient.getUserById(customerId)
                .orElseThrow(() -> {
                    log.warn("user-service lookup empty customerId={}", customerId);
                    return new ResourceNotFoundException(
                            "User not found in user-service with id: " + customerId);
                });

        // 2. restaurant-service: confirm the restaurant accepts orders.
        AvailabilityResponse availability = restaurantClient.checkAvailability(restaurantUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Restaurant not found in restaurant-service with id: " + restaurantUuid));
        if (!availability.acceptsOrders()) {
            log.warn("restaurant not accepting orders restaurantId={} isOpen={}",
                    restaurantUuid, availability.isOpen());
            throw new IllegalArgumentException(
                    "Restaurant " + restaurantUuid + " is not accepting orders right now");
        }

        // 3. menu-service: validate items and obtain authoritative pricing.
        List<ValidateMenuItemsRequest.Line> lookupLines = request.items().stream()
                .map(i -> new ValidateMenuItemsRequest.Line(
                        parseMenuItemId(i.menuItemId()), i.quantity()))
                .toList();
        ValidateMenuItemsResponse validation = menuClient.validate(new ValidateMenuItemsRequest(lookupLines))
                .orElseThrow(() -> new IllegalStateException(
                        "menu-service validate call failed"));
        if (!validation.allValid() || validation.items() == null) {
            log.warn("menu validation rejected order restaurantId={} allValid={}",
                    restaurantUuid, validation.allValid());
            throw new IllegalArgumentException(
                    "One or more menu items are invalid or unavailable");
        }
        Map<UUID, ValidateMenuItemsResponse.Line> pricedByMenuItemId = new HashMap<>();
        for (ValidateMenuItemsResponse.Line line : validation.items()) {
            pricedByMenuItemId.put(line.menuItemId(), line);
        }

        // 4. Build the entity using server-side prices, then persist as PLACED.
        Order order = new Order();
        order.setUserId(externalUser.userId());
        order.setRestaurantId(request.restaurantId());
        order.setStatus(OrderServiceConstants.STATUS_PLACED);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (OrderLineRequest req : request.items()) {
            UUID menuItemUuid = parseMenuItemId(req.menuItemId());
            ValidateMenuItemsResponse.Line priced = pricedByMenuItemId.get(menuItemUuid);
            if (priced == null) {
                // Shouldn't happen because allValid was true; defend anyway.
                throw new IllegalStateException(
                        "menu-service did not return pricing for item " + menuItemUuid);
            }
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setMenuItemId(req.menuItemId());
            item.setName(req.name());
            item.setUnitPrice(priced.unitPriceAmount());
            item.setQuantity(req.quantity());
            items.add(item);
            total = total.add(priced.unitPriceAmount().multiply(BigDecimal.valueOf(req.quantity())));
        }
        order.setItems(items);
        order.setTotalAmount(total);

        Order savedOrder = orderRepository.save(order);
        log.info("order persisted orderId={} customerId={} restaurantId={} items={} total={} status={}",
                savedOrder.getOrderId(), customerId, restaurantUuid,
                items.size(), total, OrderServiceConstants.STATUS_PLACED);

        // 5. payment-service: synchronous charge. payment-service expects a
        //    UUID; derive one from the Long PK so /payments/by-order/{id}
        //    stays stable across retries.
        final Long orderPk = savedOrder.getOrderId();
        UUID paymentOrderId = new UUID(0L, orderPk);
        PaymentResponse payment = paymentClient.createPayment(
                        new CreatePaymentRequest(paymentOrderId, total, "EUR"))
                .orElseThrow(() -> new IllegalStateException(
                        "payment-service did not return a payment for orderId=" + orderPk));
        log.info("payment response paymentId={} orderId={} status={}",
                payment.paymentId(), orderPk, payment.status());
        if (!OrderServiceConstants.PAYMENT_STATUS_COMPLETED.equalsIgnoreCase(payment.status())) {
            throw new IllegalStateException(
                    "Payment did not complete (status=" + payment.status() + ")");
        }
        savedOrder.setStatus(OrderServiceConstants.STATUS_PAID);
        savedOrder = orderRepository.save(savedOrder);
        log.info("order status -> PAID orderId={}", orderPk);

        // 6. delivery-service: schedule the delivery task. Pickup address
        //    comes from restaurant-service; dropoff from the request.
        String pickupAddress = resolvePickupAddress(restaurantUuid);
        DeliveryResponse delivery = deliveryClient.createDelivery(
                        new CreateDeliveryRequest(paymentOrderId, pickupAddress, request.deliveryAddress()))
                .orElseThrow(() -> new IllegalStateException(
                        "delivery-service did not return a delivery for orderId=" + orderPk));
        log.info("delivery scheduled deliveryId={} orderId={} status={}",
                delivery.deliveryId(), orderPk, delivery.status());

        savedOrder.setStatus(OrderServiceConstants.STATUS_CONFIRMED);
        savedOrder = orderRepository.save(savedOrder);
        log.info("order status -> CONFIRMED orderId={}", orderPk);

        return orderMapper.mapToResponse(savedOrder);
    }

    private String resolvePickupAddress(UUID restaurantId) {
        RestaurantSummaryResponse r = restaurantClient.getRestaurant(restaurantId).orElse(null);
        if (r == null) {
            // The availability check just passed, so the record exists -- but
            // detail lookup could still fail transiently. Use a stable fallback
            // rather than 5xx'ing a paid order at the delivery step.
            log.warn("pickup address fallback: restaurant lookup empty restaurantId={}", restaurantId);
            return "Restaurant " + restaurantId;
        }
        String addr = r.address();
        String city = r.city();
        if (addr == null && city == null) {
            return r.name() != null ? r.name() : ("Restaurant " + restaurantId);
        }
        if (addr == null) return city;
        if (city == null) return addr;
        return addr + ", " + city;
    }

    private static UUID parseRestaurantId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("restaurantId is required");
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("restaurantId is not a valid UUID: " + raw);
        }
    }

    private static UUID parseMenuItemId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("menuItemId is required on each line");
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("menuItemId is not a valid UUID: " + raw);
        }
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

        if (!OrderServiceConstants.STATUS_PLACED.equalsIgnoreCase(order.getStatus())
                && !OrderServiceConstants.STATUS_PENDING.equalsIgnoreCase(order.getStatus())) {
            log.warn("cancel rejected orderId={} currentStatus={} (only PLACED/PENDING are cancellable)",
                    id, order.getStatus());
            throw new IllegalStateException("Only placed orders can be cancelled");
        }

        order.setStatus(OrderServiceConstants.STATUS_CANCELLED);
        orderRepository.save(order);
        log.info("order cancelled orderId={}", id);
    }

    @Override
    @Transactional
    public OrderResponse acceptOrder(Long id, UUID actorUserId) {
        return transitionByOwner(id, actorUserId, OrderServiceConstants.STATUS_ACCEPTED, "accept");
    }

    @Override
    @Transactional
    public OrderResponse rejectOrder(Long id, UUID actorUserId) {
        return transitionByOwner(id, actorUserId, OrderServiceConstants.STATUS_REJECTED, "reject");
    }

    /**
     * Shared accept/reject logic. Owners can only act on CONFIRMED orders --
     * earlier statuses haven't completed payment+delivery, and later statuses
     * mean the kitchen has already moved on. Ownership is verified by
     * looking up the restaurant via restaurant-service, which itself denies
     * non-owners with 403 (returned to our RestaurantClient as empty
     * Optional); we additionally double-check the ownerId on the response
     * to defend against any future relaxation of that endpoint.
     */
    private OrderResponse transitionByOwner(Long id, UUID actorUserId, String targetStatus, String action) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (!OrderServiceConstants.STATUS_CONFIRMED.equalsIgnoreCase(order.getStatus())) {
            log.warn("{} rejected orderId={} currentStatus={} (only CONFIRMED is actionable)",
                    action, id, order.getStatus());
            throw new IllegalStateException(
                    "Only CONFIRMED orders can be " + targetStatus.toLowerCase()
                            + " (currentStatus=" + order.getStatus() + ")");
        }

        UUID restaurantUuid;
        try {
            restaurantUuid = UUID.fromString(order.getRestaurantId());
        } catch (IllegalArgumentException e) {
            // Existing order data: malformed restaurantId -- we can't check
            // ownership, so refuse rather than guess.
            throw new IllegalStateException("Order has malformed restaurantId: " + order.getRestaurantId());
        }
        var restaurant = restaurantClient.getRestaurant(restaurantUuid)
                .orElseThrow(() -> new AccessDeniedException(
                        "Cannot " + action + " order " + id + ": restaurant lookup failed or access denied"));
        if (restaurant.ownerId() == null || !restaurant.ownerId().equals(actorUserId)) {
            log.warn("{} denied orderId={} restaurantId={} actor={} ownerId={}",
                    action, id, restaurantUuid, actorUserId, restaurant.ownerId());
            throw new AccessDeniedException(
                    "User " + actorUserId + " does not own restaurant " + restaurantUuid);
        }

        order.setStatus(targetStatus);
        Order saved = orderRepository.save(order);
        log.info("order {} -> {} orderId={} actor={}", action, targetStatus, id, actorUserId);
        return orderMapper.mapToResponse(saved);
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
