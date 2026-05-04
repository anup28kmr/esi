package ee.ut.ege.paymentservice.service;

import ee.ut.ege.paymentservice.domain.*;
import ee.ut.ege.paymentservice.dto.*;
import ee.ut.ege.paymentservice.exception.*;
import ee.ut.ege.paymentservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        if (paymentRepository.existsByOrderId(request.getOrderId())) {
            throw new DuplicatePaymentException(
                    "Payment already exists for order: " + request.getOrderId());
        }

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        Transaction charge = Transaction.builder()
                .payment(payment)
                .type(TransactionType.CHARGE)
                .amount(request.getAmount())
                .occurredAt(Instant.now())
                .build();

        transactionRepository.save(charge);

        // Simulated payment processing — always succeeds
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedAt(Instant.now());
        payment = paymentRepository.save(payment);

        eventPublisher.publishPaymentCompleted(
                payment.getPaymentId(), payment.getOrderId(), payment.getAmount());

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        return toResponse(findById(paymentId));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "No payment found for order: " + orderId));
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(UUID paymentId) {
        Payment payment = findById(paymentId);

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidPaymentStateException("Only completed payments can be refunded");
        }

        Transaction refund = Transaction.builder()
                .payment(payment)
                .type(TransactionType.REFUND)
                .amount(payment.getAmount())
                .occurredAt(Instant.now())
                .build();

        transactionRepository.save(refund);

        payment.setStatus(PaymentStatus.REFUNDED);
        payment = paymentRepository.save(payment);

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(UUID paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw new PaymentNotFoundException("Payment not found: " + paymentId);
        }
        return transactionRepository.findByPayment_PaymentId(paymentId).stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    private Payment findById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .processedAt(payment.getProcessedAt())
                .build();
    }

    private TransactionResponse toTransactionResponse(Transaction tx) {
        return TransactionResponse.builder()
                .transactionId(tx.getTransactionId())
                .paymentId(tx.getPayment().getPaymentId())
                .type(tx.getType())
                .amount(tx.getAmount())
                .occurredAt(tx.getOccurredAt())
                .build();
    }
}
