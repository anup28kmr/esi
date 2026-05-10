package ee.ut.ege.deliveryservice.client;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class PaymentClientResponse {
    private UUID paymentId;
    private UUID orderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private Instant processedAt;
}
