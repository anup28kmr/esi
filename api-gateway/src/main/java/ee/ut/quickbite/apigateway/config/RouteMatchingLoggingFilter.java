package ee.ut.quickbite.apigateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter for logging route-matching information in the API Gateway.
 * Logs details about which routes are being matched for incoming requests.
 */
@Component
@Slf4j
public class RouteMatchingLoggingFilter extends OncePerRequestFilter {

  public static final String MATCHED_ROUTE_ID = "matchedRouteId";
  public static final String MATCHED_ROUTE_URI = "matchedRouteUri";

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String method = request.getMethod();
    String path = request.getRequestURI();

    try {
      filterChain.doFilter(request, response);
    } finally {
      // Log route-matching information if available
      String routeId = (String) request.getAttribute(MATCHED_ROUTE_ID);
      String routeUri = (String) request.getAttribute(MATCHED_ROUTE_URI);

      if (routeId != null || routeUri != null) {
        log.debug(
            "Route matched: method={}, path={}, routeId={}, targetUri={}",
            method,
            path,
            routeId,
            routeUri);
      }
    }
  }
}

