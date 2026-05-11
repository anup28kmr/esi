package ee.ut.quickbite.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;

/**
 * Configuration to ensure API requests bypass Spring's static resource handling
 * and go through proper gateway routing instead.
 */
@Configuration
@Slf4j
public class ApiRouteConfiguration implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Set high order to prevent Spring from treating /api/** as static resources
    registry.setOrder(Integer.MAX_VALUE);
  }

  /**
   * Filter to debug and enforce that /api/** requests are routed through the gateway,
   * not treated as static resources.
   */
  @Bean
  public FilterRegistrationBean<ApiRequestDebugFilter> apiRequestDebugFilter() {
    FilterRegistrationBean<ApiRequestDebugFilter> registrationBean =
        new FilterRegistrationBean<>(new ApiRequestDebugFilter());
    registrationBean.addUrlPatterns("/api/*");
    registrationBean.setOrder(-1); // Run early
    return registrationBean;
  }

  @Slf4j
  public static class ApiRequestDebugFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain)
        throws ServletException, IOException {
      String method = request.getMethod();
      String path = request.getRequestURI();

      log.debug("API request intercepted by ApiRequestDebugFilter: method={}, path={}", method, path);

      try {
        filterChain.doFilter(request, response);
      } finally {
        log.debug("API request processed by ApiRequestDebugFilter: method={}, path={}, status={}",
            method, path, response.getStatus());
      }
    }
  }
}

