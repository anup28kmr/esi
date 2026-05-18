package ee.ut.quickbite.orderservice.dto.external;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        String currency,
        String status,
        Instant processedAt
) {
}
