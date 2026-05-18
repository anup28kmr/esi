package ee.ut.quickbite.orderservice.dto.external;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        UUID orderId,
        BigDecimal amount,
        String currency
) {
}
