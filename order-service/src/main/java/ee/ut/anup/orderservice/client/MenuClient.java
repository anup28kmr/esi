package ee.ut.anup.orderservice.client;

import ee.ut.anup.orderservice.dto.external.ValidateMenuItemsRequest;
import ee.ut.anup.orderservice.dto.external.ValidateMenuItemsResponse;
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
 * Talks to menu-service. Used by the order placement flow to confirm each
 * line refers to an existing, available item and to obtain authoritative
 * unit prices -- the client-supplied price on OrderLineRequest is ignored.
 */
@Component
@Slf4j
public class MenuClient {

    private final RestClient restClient;

    public MenuClient(RestClient.Builder restClientBuilder,
                      @Value("${menu-service.url:http://localhost:8082}") String menuServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(menuServiceUrl).build();
        log.info("MenuClient base-url={}", menuServiceUrl);
    }

    public Optional<ValidateMenuItemsResponse> validate(ValidateMenuItemsRequest request) {
        String authHeader = currentAuthorizationHeader();
        if (authHeader == null) {
            log.warn("menu-service validate skipped: no Authorization header on current request");
            return Optional.empty();
        }
        try {
            ValidateMenuItemsResponse body = restClient.post()
                    .uri("/menu-items/validate")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ValidateMenuItemsResponse.class);
            return Optional.ofNullable(body);
        } catch (Exception e) {
            log.warn("menu-service validate failed itemCount={} error={}",
                    request.items() == null ? 0 : request.items().size(), e.getMessage());
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
