package ee.ut.ege.paymentservice.dto;

import ee.ut.ege.paymentservice.domain.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TransactionResponse {
    private UUID transactionId;
    private UUID paymentId;
    private TransactionType type;
    private BigDecimal amount;
    private Instant occurredAt;
}
