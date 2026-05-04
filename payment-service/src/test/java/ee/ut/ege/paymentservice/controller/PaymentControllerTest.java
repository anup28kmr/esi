package ee.ut.ege.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.ut.ege.paymentservice.domain.PaymentStatus;
import ee.ut.ege.paymentservice.dto.CreatePaymentRequest;
import ee.ut.ege.paymentservice.dto.PaymentResponse;
import ee.ut.ege.paymentservice.domain.TransactionType;
import ee.ut.ege.paymentservice.dto.TransactionResponse;
import ee.ut.ege.paymentservice.exception.DuplicatePaymentException;
import ee.ut.ege.paymentservice.exception.PaymentNotFoundException;
import ee.ut.ege.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests PaymentController with PaymentService mocked.
 * PaymentService is the dependency through which this endpoint interacts
 * with the PaymentEventPublisher (Kafka component) and the persistence layer.
 * Mocking PaymentService isolates the controller and verifies it correctly
 * delegates calls and maps responses/errors.
 */
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void createPayment_validRequest_returns201WithCompletedStatus() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(orderId);
        request.setAmount(new BigDecimal("25.50"));
        request.setCurrency("EUR");

        PaymentResponse response = PaymentResponse.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .amount(new BigDecimal("25.50"))
                .currency("EUR")
                .status(PaymentStatus.COMPLETED)
                .processedAt(Instant.now())
                .build();

        when(paymentService.createPayment(any())).thenReturn(response);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void getPayment_existingId_returns200() throws Exception {
        UUID paymentId = UUID.randomUUID();

        PaymentResponse response = PaymentResponse.builder()
                .paymentId(paymentId)
                .orderId(UUID.randomUUID())
                .amount(new BigDecimal("15.00"))
                .currency("EUR")
                .status(PaymentStatus.COMPLETED)
                .processedAt(Instant.now())
                .build();

        when(paymentService.getPayment(paymentId)).thenReturn(response);

        mockMvc.perform(get("/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getTransactions_existingPayment_returnsList() throws Exception {
        UUID paymentId = UUID.randomUUID();

        TransactionResponse tx = TransactionResponse.builder()
                .transactionId(UUID.randomUUID())
                .paymentId(paymentId)
                .type(TransactionType.CHARGE)
                .amount(new BigDecimal("25.50"))
                .occurredAt(Instant.now())
                .build();

        when(paymentService.getTransactions(paymentId)).thenReturn(List.of(tx));

        mockMvc.perform(get("/payments/{id}/transactions", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[0].paymentId").value(paymentId.toString()));
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    void createPayment_duplicateOrder_returns409() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(UUID.randomUUID());
        request.setAmount(new BigDecimal("25.50"));
        request.setCurrency("EUR");

        when(paymentService.createPayment(any()))
                .thenThrow(new DuplicatePaymentException("Payment already exists for this order"));

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Payment already exists for this order"));
    }

    @Test
    void getPayment_nonExistentId_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();

        when(paymentService.getPayment(unknownId))
                .thenThrow(new PaymentNotFoundException("Payment not found: " + unknownId));

        mockMvc.perform(get("/payments/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createPayment_missingOrderId_returns400() throws Exception {
        // orderId is null — validation should reject it
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(new BigDecimal("10.00"));
        request.setCurrency("EUR");

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
