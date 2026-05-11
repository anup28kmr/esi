package ee.ut.anup.orderservice.controller;

import ee.ut.anup.orderservice.dto.*;
import ee.ut.anup.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Order management API")
@AllArgsConstructor
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
            @Parameter(description = "Authenticated customer id from User Service", required = true)
            @RequestHeader("X-User-Id") Long customerId,
            @RequestBody PlaceOrderRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(customerId, request));
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
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }

    @Operation(summary = "List customer orders", description = "List the orders of a given customer (order history).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders listed",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class))))
    })
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@RequestParam Long customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
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
        return ResponseEntity.ok(orderService.getOrderItems(id));
    }
}