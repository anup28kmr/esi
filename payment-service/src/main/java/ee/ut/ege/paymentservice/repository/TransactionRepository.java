package ee.ut.ege.paymentservice.repository;

import ee.ut.ege.paymentservice.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByPayment_PaymentId(UUID paymentId);
}
