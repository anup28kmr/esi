package ee.ut.quickbite.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Enumeration;

/**
 * Fallback controller for gateway routing in servlet mode.
 * This ensures that API requests are properly routed to backend services
 * even if the gateway-server-webmvc route configuration doesn't catch them.
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class ApiGatewayController {

  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${USER_SERVICE_HOST:user-service}")
  private String userServiceHost;

  @Value("${USER_SERVICE_PORT:7000}")
  private int userServicePort;

  /**
   * Route /api/** requests to the appropriate backend services.
   */
  @RequestMapping("/**")
  public ResponseEntity<?> routeRequest(HttpServletRequest request) {
    String method = request.getMethod();
    String path = request.getRequestURI();
    String originalPath = request.getRequestURI();

    log.info("Gateway controller intercepted: method={}, path={}", method, path);

    try {
      // Extract the service type from the path and route accordingly
      if (path.contains("/users") || path.contains("/auth")) {
        String routeId = path.contains("/users") ? "user-service-api-users" : "user-service-api-auth";
        return routeToUserService(request, originalPath, routeId);
      } else if (path.contains("/orders")) {
        return routeToOrderService(request, originalPath, "order-service");
      } else if (path.contains("/restaurants")) {
        return routeToRestaurantService(request, originalPath, "restaurant-service");
      } else if (path.contains("/menu-items")) {
        return routeToMenuService(request, originalPath, "menu-service");
      } else if (path.contains("/payments")) {
        return routeToPaymentService(request, originalPath, "payment-service");
      } else if (path.contains("/deliveries")) {
        return routeToDeliveryService(request, originalPath, "delivery-service");
      } else if (path.contains("/notifications")) {
        return routeToNotificationService(request, originalPath, "notification-service");
      }

      log.warn("No route found for path: {}", originalPath);
      return ResponseEntity.notFound().build();

    } catch (Exception e) {
      log.error("Error routing request: method={}, path={}, error={}", method, path, e.getMessage(), e);
      return ResponseEntity.status(500).body("Gateway routing error: " + e.getMessage());
    }
  }

  private void setRouteAttributes(HttpServletRequest request, String routeId, String targetUri) {
    request.setAttribute("ee.ut.quickbite.apigateway.config.RouteMatchingLoggingFilter.MATCHED_ROUTE_ID", routeId);
    request.setAttribute("ee.ut.quickbite.apigateway.config.RouteMatchingLoggingFilter.MATCHED_ROUTE_URI", targetUri);
  }

  private ResponseEntity<?> routeToUserService(HttpServletRequest request, String originalPath, String routeId)
      throws Exception {
    String targetPath = originalPath.replaceFirst("/api", "");
    String targetUrl = String.format("http://%s:%d%s", userServiceHost, userServicePort, targetPath);

    setRouteAttributes(request, routeId, targetUrl);
    log.debug("Routing to user-service: {}", targetUrl);
    return forwardRequest(request, targetUrl);
  }

  private ResponseEntity<?> routeToOrderService(HttpServletRequest request, String originalPath, String routeId)
      throws Exception {
    String targetPath = originalPath.replaceFirst("/api", "");
    String targetUrl = String.format("http://order-service:7001%s", targetPath);

    setRouteAttributes(request, routeId, targetUrl);
    log.debug("Routing to order-service: {}", targetUrl);
    return forwardRequest(request, targetUrl);
  }

  private ResponseEntity<?> routeToRestaurantService(HttpServletRequest request, String originalPath, String routeId)
      throws Exception {
    String targetPath = originalPath.replaceFirst("/api", "");
    String targetUrl = String.format("http://restaurant-service:8081%s", targetPath);

    setRouteAttributes(request, routeId, targetUrl);
    log.debug("Routing to restaurant-service: {}", targetUrl);
    return forwardRequest(request, targetUrl);
  }

  private ResponseEntity<?> routeToMenuService(HttpServletRequest request, String originalPath, String routeId)
      throws Exception {
    String targetPath = originalPath.replaceFirst("/api", "");
    String targetUrl = String.format("http://menu-service:8082%s", targetPath);

    setRouteAttributes(request, routeId, targetUrl);
    log.debug("Routing to menu-service: {}", targetUrl);
    return forwardRequest(request, targetUrl);
  }

  private ResponseEntity<?> routeToPaymentService(HttpServletRequest request, String originalPath, String routeId)
      throws Exception {
    String targetPath = originalPath.replaceFirst("/api", "");
    String targetUrl = String.format("http://payment-service:8085%s", targetPath);

    setRouteAttributes(request, routeId, targetUrl);
    log.debug("Routing to payment-service: {}", targetUrl);
    return forwardRequest(request, targetUrl);
  }

  private ResponseEntity<?> routeToDeliveryService(HttpServletRequest request, String originalPath, String routeId)
      throws Exception {
    String targetPath = originalPath.replaceFirst("/api", "");
    String targetUrl = String.format("http://delivery-service:8086%s", targetPath);

    setRouteAttributes(request, routeId, targetUrl);
    log.debug("Routing to delivery-service: {}", targetUrl);
    return forwardRequest(request, targetUrl);
  }

  private ResponseEntity<?> routeToNotificationService(HttpServletRequest request, String originalPath, String routeId)
      throws Exception {
    String targetPath = originalPath.replaceFirst("/api", "");
    String targetUrl = String.format("http://notification-service:8087%s", targetPath);

    setRouteAttributes(request, routeId, targetUrl);
    log.debug("Routing to notification-service: {}", targetUrl);
    return forwardRequest(request, targetUrl);
  }

  private ResponseEntity<?> forwardRequest(HttpServletRequest request, String targetUrl)
      throws Exception {
    HttpMethod method = HttpMethod.valueOf(request.getMethod());

    // Build headers
    HttpHeaders headers = new HttpHeaders();
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      String headerValue = request.getHeader(headerName);
      // Skip headers that might cause issues
      if (!headerName.equalsIgnoreCase("host") && !headerName.equalsIgnoreCase("content-length")) {
        headers.add(headerName, headerValue);
      }
    }

    // Get request body if present
    String body = null;
    if (request.getContentLength() > 0) {
      body = new String(request.getInputStream().readAllBytes());
    }

    HttpEntity<?> httpEntity = new HttpEntity<>(body, headers);

    log.info("Forwarding request: method={}, url={}", method, targetUrl);

    return restTemplate.exchange(new URI(targetUrl), method, httpEntity, Object.class);
  }
}

