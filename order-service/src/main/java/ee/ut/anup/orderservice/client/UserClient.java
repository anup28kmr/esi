package ee.ut.anup.orderservice.client;

import ee.ut.anup.orderservice.dto.external.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Talks to user-service. Forwards the incoming request's Authorization
 * header so the call is performed on behalf of the authenticated user --
 * matches the "verified from user-service on every round trip" design.
 */
@Component
@Slf4j
public class UserClient {

    private final RestClient restClient;

    public UserClient(RestClient.Builder restClientBuilder,
                      @Value("${user-service.url:http://localhost:7000}") String userServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(userServiceUrl).build();
        log.info("UserClient base-url={}", userServiceUrl);
    }

    /**
     * Calls user-service /auth/validateToken with the current request's JWT.
     * Returns the user if the token is still valid AND the user is ACTIVE;
     * empty Optional on any auth failure (bad signature, expired, suspended,
     * I/O error). The UUID id parameter is the value the controller pulled
     * from the JWT principal; we cross-check that user-service's
     * introspection returns the same id, to defend against token
     * substitution (token A presented while header claims to be user B).
     */
    public Optional<UserDTO> getUserById(UUID userId) {
        String authHeader = currentAuthorizationHeader();
        if (authHeader == null) {
            log.warn("user-service introspection skipped: no Authorization header on current request");
            return Optional.empty();
        }
        try {
            UserDTO user = restClient.get()
                    .uri("/auth/validateToken")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(UserDTO.class);
            if (user == null) {
                log.warn("user-service introspection returned empty body for expected userId={}", userId);
                return Optional.empty();
            }
            if (!Objects.equals(user.userId(), userId)) {
                log.warn("introspection mismatch: token user={} but controller used customerId={}",
                        user.userId(), userId);
                return Optional.empty();
            }
            return Optional.of(user);
        } catch (Exception e) {
            log.warn("user-service introspection failed userId={} error={}", userId, e.getMessage());
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
