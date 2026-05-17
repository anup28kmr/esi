package ee.ut.ege.deliveryservice.controller;

import ee.ut.ege.deliveryservice.dto.DeliveryResponse;
import ee.ut.ege.deliveryservice.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
@Tag(name = "Drivers", description = "Driver-level delivery queries")
public class DriverController {

    private final DeliveryService deliveryService;

    @GetMapping("/{id}/active")
    @Operation(summary = "Get the active delivery (Assigned / PickedUp / InTransit) of a given driver")
    public ResponseEntity<DeliveryResponse> getActiveDelivery(@PathVariable UUID id) {
        return ResponseEntity.ok(deliveryService.getActiveDeliveryForDriver(id));
    }
}
