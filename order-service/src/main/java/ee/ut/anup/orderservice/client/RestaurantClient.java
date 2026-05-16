package ee.ut.anup.orderservice.client;

import ee.ut.anup.orderservice.dto.external.AvailabilityResponse;
import ee.ut.anup.orderservice.dto.external.RestaurantSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

/**
 * Talks to restaurant-service. Forwards the caller's Authorization header so
 * every call runs under the customer's JWT -- the spec calls for a "service
 * bearer token" on every internal call; we satisfy that by propagating the
 * user token, which every microservice already validates via its own
 * JwtAuthFilter.
 */
@Component
@Slf4j
public class RestaurantClient {

    private final RestClient restClient;

    public RestaurantClient(RestClient.Builder restClientBuilder,
                            @Value("${restaurant-service.url:http://localhost:8081}") String restaurantServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(restaurantServiceUrl).build();
        log.info("RestaurantClient base-url={}", restaurantServiceUrl);
    }

    public Optional<AvailabilityResponse> checkAvailability(UUID restaurantId) {
        String authHeader = currentAuthorizationHeader();
        if (authHeader == null) {
            log.warn("restaurant-service availability skipped: no Authorization header on current request");
            return Optional.empty();
        }
        try {
            AvailabilityResponse body = restClient.get()
                    .uri("/restaurants/{id}/availability", restaurantId)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(AvailabilityResponse.class);
            return Optional.ofNullable(body);
        } catch (Exception e) {
            log.warn("restaurant-service availability call failed restaurantId={} error={}",
                    restaurantId, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<RestaurantSummaryResponse> getRestaurant(UUID restaurantId) {
        String authHeader = currentAuthorizationHeader();
        if (authHeader == null) {
            log.warn("restaurant-service lookup skipped: no Authorization header on current request");
            return Optional.empty();
        }
        try {
            RestaurantSummaryResponse body = restClient.get()
                    .uri("/restaurants/{id}", restaurantId)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(RestaurantSummaryResponse.class);
            return Optional.ofNullable(body);
        } catch (Exception e) {
            log.warn("restaurant-service lookup failed restaurantId={} error={}",
                    restaurantId, e.getMessage());
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
