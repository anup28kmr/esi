package ee.ut.quickbite.apigateway.client;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calls user-service /auth/validateToken to introspect a bearer JWT. The
 * gateway runs this on every protected request, so we cache successful
 * results for {@value #CACHE_TTL_SECONDS}s keyed by SHA-256(token) to avoid
 * hammering user-service. Failures (401/network) are never cached: they
 * stay cheap to retry and we never want a transient blip to lock a user out
 * for longer than necessary.
 */
@Component
@Slf4j
public class UserIntrospectionClient {

    private static final long CACHE_TTL_SECONDS = 60;
    private static final int CACHE_MAX_ENTRIES = 5_000;

    private final RestClient http;
    private final ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<>();

    public UserIntrospectionClient(
        RestClient.Builder builder,
        @Value("${USER_SERVICE_HOST:user-service}") String host,
        @Value("${USER_SERVICE_PORT:7000}") int port
    ) {
        String baseUrl = "http://" + host + ":" + port;
        this.http = builder.baseUrl(baseUrl).build();
        log.info("UserIntrospectionClient base-url={}", baseUrl);
    }

    @PostConstruct
    void verifyDigest() {
        // Fail fast at startup if SHA-256 isn't available -- prevents per-request surprises.
        try { MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }

    /**
     * @return true if the token validates AND the user is ACTIVE per user-service.
     */
    public boolean isValid(String bearerToken) {
        String key = digest(bearerToken);
        Long cachedUntil = cache.get(key);
        long now = Instant.now().getEpochSecond();
        if (cachedUntil != null && cachedUntil > now) {
            return true;
        }
        if (cachedUntil != null) {
            cache.remove(key);
        }
        boolean ok = callUserService(bearerToken);
        if (ok) {
            if (cache.size() >= CACHE_MAX_ENTRIES) {
                // Crude bound: drop everything rather than evict cleverly.
                // Cache rebuilds in <60s under any real load.
                cache.clear();
            }
            cache.put(key, now + CACHE_TTL_SECONDS);
        }
        return ok;
    }

    private boolean callUserService(String bearerToken) {
        try {
            // The Map<?,?> return type forces a JSON body parse; if user-service
            // returns 401, RestClient throws and we fall into the catch below.
            Map<?, ?> body = http.get()
                .uri("/auth/validateToken")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .retrieve()
                .body(Map.class);
            return body != null && body.get("userId") != null;
        } catch (Exception e) {
            log.debug("introspection failed: {}", e.getMessage());
            return false;
        }
    }

    private static String digest(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // verifyDigest() guarantees this branch is unreachable.
            throw new IllegalStateException(e);
        }
    }
}
