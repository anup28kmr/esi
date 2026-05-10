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

@Component
@Slf4j
public class GatewayRequestLoggingFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    long startNs = System.nanoTime();
    String method = request.getMethod();
    String path = request.getRequestURI();
    String query = request.getQueryString();
    String origin = request.getHeader("Origin");
    String remoteAddr = request.getRemoteAddr();

    log.info(
        "API gateway request started: method={}, path={}, query={}, origin={}, remoteAddr={}",
        method,
        path,
        query,
        origin,
        remoteAddr);

    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = (System.nanoTime() - startNs) / 1_000_000;
      String routeId = (String) request.getAttribute(RouteMatchingLoggingFilter.MATCHED_ROUTE_ID);
      String routeUri = (String) request.getAttribute(RouteMatchingLoggingFilter.MATCHED_ROUTE_URI);

      log.info(
          "API gateway request completed: method={}, path={}, status={}, durationMs={}, routeId={}, targetUri={}",
          method,
          path,
          response.getStatus(),
          durationMs,
          routeId,
          routeUri);
    }
  }
}

