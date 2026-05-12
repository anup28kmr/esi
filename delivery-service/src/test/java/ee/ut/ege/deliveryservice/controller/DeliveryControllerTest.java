package ee.ut.ege.deliveryservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ee.ut.ege.deliveryservice.domain.DeliveryStatus;
import ee.ut.ege.deliveryservice.dto.*;
import ee.ut.ege.deliveryservice.exception.DeliveryNotFoundException;
import ee.ut.ege.deliveryservice.exception.InvalidDeliveryStateException;
import ee.ut.ege.deliveryservice.exception.PaymentNotCompletedException;
import ee.ut.ege.deliveryservice.service.DeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests DeliveryController with DeliveryService mocked.
 * DeliveryService is the dependency through which the controller interacts
 * with the repository and the PaymentClient (cross-service call).
 * Mocking it isolates the controller and verifies HTTP status codes,
 * JSON mapping, and error handling.
 */
@WebMvcTest(DeliveryController.class)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private DeliveryService deliveryService;

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void createDelivery_validRequest_returns201() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setOrderId(orderId);
        request.setPickupAddress("Restaurant St 1, Tartu");
        request.setDeliveryAddress("Customer Ave 5, Tartu");

        DeliveryResponse response = buildResponse(deliveryId, orderId, DeliveryStatus.PENDING);
        when(deliveryService.createDelivery(any())).thenReturn(response);

        mockMvc.perform(post("/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryId").value(deliveryId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getDelivery_existingId_returns200() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        DeliveryResponse response = buildResponse(deliveryId, UUID.randomUUID(), DeliveryStatus.PENDING);

        when(deliveryService.getDelivery(deliveryId)).thenReturn(response);

        mockMvc.perform(get("/deliveries/{id}", deliveryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId").value(deliveryId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getDeliveryByOrder_existingOrderId_returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        DeliveryResponse response = buildResponse(UUID.randomUUID(), orderId, DeliveryStatus.ASSIGNED);

        when(deliveryService.getDeliveryByOrderId(orderId)).thenReturn(response);

        mockMvc.perform(get("/deliveries/by-order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    void assignDriver_pendingDelivery_returnsAssignedStatus() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        AssignDriverRequest request = new AssignDriverRequest();
        request.setDriverId(driverId);

        DeliveryResponse response = buildResponse(deliveryId, UUID.randomUUID(), DeliveryStatus.ASSIGNED);
        response.setDriverId(driverId);

        when(deliveryService.assignDriver(eq(deliveryId), any())).thenReturn(response);

        mockMvc.perform(post("/deliveries/{id}/assign", deliveryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.driverId").value(driverId.toString()));
    }

    @Test
    void updateStatus_validTransition_returns200() throws Exception {
        UUID deliveryId = UUID.randomUUID();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        request.setStatus(DeliveryStatus.PICKED_UP);

        DeliveryResponse response = buildResponse(deliveryId, UUID.randomUUID(), DeliveryStatus.PICKED_UP);
        when(deliveryService.updateStatus(eq(deliveryId), any())).thenReturn(response);

        mockMvc.perform(patch("/deliveries/{id}/status", deliveryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"));
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    void createDelivery_paymentNotCompleted_returns402() throws Exception {
        CreateDeliveryRequest request = new CreateDeliveryRequest();
        request.setOrderId(UUID.randomUUID());
        request.setPickupAddress("A");
        request.setDeliveryAddress("B");

        when(deliveryService.createDelivery(any()))
                .thenThrow(new PaymentNotCompletedException("No payment found for order"));

        mockMvc.perform(post("/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("No payment found for order"));
    }

    @Test
    void getDelivery_nonExistentId_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();

        when(deliveryService.getDelivery(unknownId))
                .thenThrow(new DeliveryNotFoundException("Delivery not found: " + unknownId));

        mockMvc.perform(get("/deliveries/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updateStatus_invalidTransition_returns422() throws Exception {
        UUID deliveryId = UUID.randomUUID();

        UpdateDeliveryStatusRequest request = new UpdateDeliveryStatusRequest();
        request.setStatus(DeliveryStatus.DELIVERED);

        when(deliveryService.updateStatus(eq(deliveryId), any()))
                .thenThrow(new InvalidDeliveryStateException("Cannot transition from PENDING to DELIVERED"));

        mockMvc.perform(patch("/deliveries/{id}/status", deliveryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("Cannot transition from PENDING to DELIVERED"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private DeliveryResponse buildResponse(UUID deliveryId, UUID orderId, DeliveryStatus status) {
        return DeliveryResponse.builder()
                .deliveryId(deliveryId)
                .orderId(orderId)
                .status(status)
                .pickupAddress("Restaurant St 1, Tartu")
                .deliveryAddress("Customer Ave 5, Tartu")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
