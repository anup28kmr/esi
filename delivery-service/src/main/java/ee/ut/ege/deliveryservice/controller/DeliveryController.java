package ee.ut.ege.deliveryservice.controller;

import ee.ut.ege.deliveryservice.domain.DeliveryStatus;
import ee.ut.ege.deliveryservice.dto.*;
import ee.ut.ege.deliveryservice.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
@Tag(name = "Deliveries", description = "Delivery management API")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    @Operation(summary = "Schedule a delivery for a paid order — calls payment-service to verify COMPLETED payment")
    public ResponseEntity<DeliveryResponse> createDelivery(
            @Valid @RequestBody CreateDeliveryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deliveryService.createDelivery(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a delivery by ID")
    public ResponseEntity<DeliveryResponse> getDelivery(@PathVariable UUID id) {
        return ResponseEntity.ok(deliveryService.getDelivery(id));
    }

    @GetMapping("/by-order/{orderId}")
    @Operation(summary = "Get the delivery for a given order")
    public ResponseEntity<DeliveryResponse> getDeliveryByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(deliveryService.getDeliveryByOrderId(orderId));
    }

    @GetMapping
    @Operation(summary = "List all deliveries, optionally filtered by status")
    public ResponseEntity<List<DeliveryResponse>> getAllDeliveries(
            @RequestParam(required = false) DeliveryStatus status) {
        if (status != null) {
            return ResponseEntity.ok(deliveryService.getDeliveriesByStatus(status));
        }
        return ResponseEntity.ok(deliveryService.getAllDeliveries());
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign a driver to a PENDING delivery")
    public ResponseEntity<DeliveryResponse> assignDriver(
            @PathVariable UUID id,
            @Valid @RequestBody AssignDriverRequest request) {
        return ResponseEntity.ok(deliveryService.assignDriver(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update delivery status (PICKED_UP → IN_TRANSIT → DELIVERED / FAILED / CANCELLED)")
    public ResponseEntity<DeliveryResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeliveryStatusRequest request) {
        return ResponseEntity.ok(deliveryService.updateStatus(id, request));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an active delivery")
    public ResponseEntity<DeliveryResponse> cancelDelivery(@PathVariable UUID id) {
        return ResponseEntity.ok(deliveryService.cancelDelivery(id));
    }
}
