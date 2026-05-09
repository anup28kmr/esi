package ee.ut.ege.deliveryservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentClient {

    private final RestClient paymentRestClient;

    public Optional<PaymentClientResponse> getPaymentByOrderId(UUID orderId) {
        try {
            PaymentClientResponse response = paymentRestClient.get()
                    .uri("/payments/by-order/{orderId}", orderId)
                    .retrieve()
                    .body(PaymentClientResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            log.error("Payment service error for order {}: {} {}", orderId, e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Payment service error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Payment service unavailable for order {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Payment service unavailable: " + e.getMessage(), e);
        }
    }
}
