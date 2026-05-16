package ee.ut.anup.orderservice.client;

import ee.ut.anup.orderservice.dto.external.CreateDeliveryRequest;
import ee.ut.anup.orderservice.dto.external.DeliveryResponse;
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

/**
 * Talks to delivery-service. Called once the order is in PAID state to
 * schedule the delivery task; delivery-service itself verifies that
 * payment-service has marked the order COMPLETED before persisting.
 */
@Component
@Slf4j
public class DeliveryClient {

    private final RestClient restClient;

    public DeliveryClient(RestClient.Builder restClientBuilder,
                          @Value("${delivery-service.url:http://localhost:8086}") String deliveryServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(deliveryServiceUrl).build();
        log.info("DeliveryClient base-url={}", deliveryServiceUrl);
    }

    public Optional<DeliveryResponse> createDelivery(CreateDeliveryRequest request) {
        String authHeader = currentAuthorizationHeader();
        if (authHeader == null) {
            log.warn("delivery-service createDelivery skipped: no Authorization header on current request");
            return Optional.empty();
        }
        try {
            DeliveryResponse body = restClient.post()
                    .uri("/deliveries")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(DeliveryResponse.class);
            return Optional.ofNullable(body);
        } catch (Exception e) {
            log.warn("delivery-service createDelivery failed orderId={} error={}",
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
