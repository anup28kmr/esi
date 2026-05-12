package ee.ut.quickbite.apigateway.config;

import ee.ut.quickbite.apigateway.client.UserIntrospectionClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway-level token introspection. Every request to a protected route is
 * validated against user-service /auth/validateToken before being proxied
 * downstream. Downstream services still run their own JwtAuthFilter as
 * defense-in-depth and to populate their SecurityContext.
 *
 * <p>The public allowlist mirrors what downstream services already permit
 * anonymously: login, signup, CORS preflight, swagger/actuator, and the
 * anonymous-browse GETs on restaurants and menu items. Anything that
 * mutates state, or any /api/orders, /api/payments, /api/deliveries, or
 * /api/users (other than POST signup) endpoint, requires a valid JWT.
 */
@Component
@Slf4j
public class TokenIntrospectionInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserIntrospectionClient userClient;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // (httpMethod | "*", antPathPattern) -- order doesn't matter; first match wins.
    private static final List<PublicRoute> PUBLIC = List.of(
        new PublicRoute("OPTIONS", "/**"),
        new PublicRoute("POST",    "/api/auth/login"),
        new PublicRoute("POST",    "/api/users"),
        new PublicRoute("GET",     "/api/restaurants"),
        new PublicRoute("GET",     "/api/restaurants/*"),
        new PublicRoute("GET",     "/api/restaurants/*/menu-items"),
        new PublicRoute("GET",     "/api/menu-items/*"),
        new PublicRoute("*",       "/actuator/**"),
        new PublicRoute("*",       "/v3/api-docs/**"),
        new PublicRoute("*",       "/swagger-ui/**"),
        new PublicRoute("*",       "/swagger-ui.html")
    );

    // Boot 4 uses Jackson 3 (tools.jackson). Construct locally rather than
    // relying on DI -- the interceptor only writes a fixed-shape error body.
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public TokenIntrospectionInterceptor(UserIntrospectionClient userClient) {
        this.userClient = userClient;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws IOException {
        if (isPublic(request)) {
            return true;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("gateway 401: no bearer token for {} {}", request.getMethod(), request.getRequestURI());
            writeUnauthorized(request, response, "Authentication required");
            return false;
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty() || !userClient.isValid(token)) {
            log.debug("gateway 401: introspection failed for {} {}", request.getMethod(), request.getRequestURI());
            writeUnauthorized(request, response, "Invalid or expired token");
            return false;
        }
        return true;
    }

    private boolean isPublic(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        for (PublicRoute pr : PUBLIC) {
            boolean methodMatches = "*".equals(pr.method) || pr.method.equalsIgnoreCase(method);
            if (methodMatches && pathMatcher.match(pr.pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        try {
            objectMapper.writeValue(response.getWriter(), body);
        } catch (Exception e) {
            // Fallback: write a minimal plain-text body so we still 401 cleanly.
            response.getWriter().write("{\"status\":401,\"message\":\"" + message + "\"}");
        }
    }

    private record PublicRoute(String method, String pattern) {}
}
