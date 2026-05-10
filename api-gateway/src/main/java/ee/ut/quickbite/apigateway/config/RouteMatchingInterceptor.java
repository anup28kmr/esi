package ee.ut.quickbite.apigateway.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor for logging route information during request handling.
 * Captures and logs route details after the handler has processed the request.
 */
@Component
@Slf4j
public class RouteMatchingInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    return true;
  }

  @Override
  public void postHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler,
      @Nullable ModelAndView modelAndView) {
    String method = request.getMethod();
    String path = request.getRequestURI();
    String handlerName = handler.getClass().getSimpleName();

    log.debug(
        "Route handling: method={}, path={}, handler={}, status={}",
        method,
        path,
        handlerName,
        response.getStatus());
  }

  @Override
  public void afterCompletion(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler,
      @Nullable Exception ex) {
    if (ex != null) {
      log.warn(
          "Route handling error: method={}, path={}, status={}, error={}",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          ex.getMessage());
    }
  }
}

