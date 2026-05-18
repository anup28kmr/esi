package ee.ut.quickbite.orderservice.client;

import ee.ut.quickbite.orderservice.dto.external.CreatePaymentRequest;
import ee.ut.quickbite.orderservice.dto.external.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Component
@Slf4j
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(RestClient.Builder restClientBuilder,
                         @Value("${payment-service.url:http://localhost:8085}") String paymentServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(paymentServiceUrl).build();
        log.info("PaymentClient base-url={}", paymentServiceUrl);
    }

    public Optional<PaymentResponse> createPayment(CreatePaymentRequest request) {
        String authHeader = currentAuthorizationHeader();
        if (authHeader == null) {
            log.warn("payment-service createPayment skipped: no Authorization header on current request");
            return Optional.empty();
        }
        try {
            PaymentResponse payment = restClient.post()
                    .uri("/payments")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(PaymentResponse.class);
            return Optional.ofNullable(payment);
        } catch (Exception e) {
            log.warn("payment-service createPayment failed orderId={} error={}",
                    request.orderId(), e.getMessage());
            return Optional.empty();
        }
    }

    private static String currentAuthorizationHeader() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }
}
