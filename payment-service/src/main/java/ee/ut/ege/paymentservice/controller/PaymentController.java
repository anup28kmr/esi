package ee.ut.ege.paymentservice.controller;

import ee.ut.ege.paymentservice.dto.CreatePaymentRequest;
import ee.ut.ege.paymentservice.dto.PaymentResponse;
import ee.ut.ege.paymentservice.dto.TransactionResponse;
import ee.ut.ege.paymentservice.service.PaymentService;
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
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management API")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Start a payment charge for an order")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment and its status")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    @GetMapping("/by-order/{orderId}")
    @Operation(summary = "Get the payment that belongs to a given order")
    public ResponseEntity<PaymentResponse> getPaymentByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Issue a refund for a cancelled or failed order")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.refundPayment(id));
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "List the transactions (charge or refund) of a payment")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getTransactions(id));
    }
}
