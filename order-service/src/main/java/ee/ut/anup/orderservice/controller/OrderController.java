package ee.ut.anup.orderservice.controller;

import ee.ut.anup.orderservice.dto.*;
import ee.ut.anup.orderservice.security.AuthenticatedUser;
import ee.ut.anup.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Order management API")
@AllArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "Place a new order",
            description = "Uses authenticated customer identity, validates restaurant and menu items, starts payment, and creates delivery task."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<?> placeOrder(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody PlaceOrderRequest request
    ) {
        UUID customerId = principal.userId();
        int lineCount = request.items() == null ? 0 : request.items().size();
        log.info("POST /orders customerId={} restaurantId={} lines={}",
                customerId, request.restaurantId(), lineCount);
        OrderResponse response = orderService.placeOrder(customerId, request);
        log.info("order placed orderId={} customerId={} total={}",
                response.orderId(), customerId, response.totalAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get order by id", description = "Get details and current status of an order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        log.debug("GET /orders/{}", id);
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @Operation(summary = "Cancel an order", description = "Cancel a pending order. Only allowed before restaurant accepts it.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Order cancelled"),
            @ApiResponse(responseCode = "400", description = "Order cannot be cancelled",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        log.info("DELETE /orders/{}", id);
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Internal status change", description = "Called by Payment and Delivery event handlers to update order status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status updated",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        log.info("PATCH /orders/{}/status status='{}'", id, request.status());
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }

    @Operation(
            summary = "List orders",
            description = "List orders by customer (customerId) or by restaurant (restaurantId). "
                    + "Exactly one of customerId or restaurantId is required."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders listed",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Neither or both filters supplied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<OrderResponse>> listOrders(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String restaurantId
    ) {
        boolean hasCustomer = customerId != null;
        boolean hasRestaurant = restaurantId != null && !restaurantId.isBlank();
        if (hasCustomer == hasRestaurant) {
            log.warn("GET /orders rejected: customerId={} restaurantId={} (exactly one required)",
                    customerId, restaurantId);
            throw new IllegalArgumentException(
                    "Provide exactly one of `customerId` or `restaurantId`."
            );
        }
        log.debug("GET /orders filter={} value={}",
                hasCustomer ? "customerId" : "restaurantId",
                hasCustomer ? customerId : restaurantId);
        return ResponseEntity.ok(hasCustomer
                ? orderService.getOrdersByCustomer(customerId)
                : orderService.getOrdersByRestaurant(restaurantId));
    }

    @Operation(summary = "List order items", description = "List the items of a given order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order items listed",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderItemResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/items")
    public ResponseEntity<List<OrderItemResponse>> getOrderItems(@PathVariable Long id) {
        log.debug("GET /orders/{}/items", id);
        return ResponseEntity.ok(orderService.getOrderItems(id));
    }
}