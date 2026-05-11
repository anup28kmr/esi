package ee.ut.quickbite.apigateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for registering route-matching interceptors and filters.
 */
@Configuration
public class GatewayInterceptorConfig implements WebMvcConfigurer {

  private final RouteMatchingInterceptor routeMatchingInterceptor;

  public GatewayInterceptorConfig(RouteMatchingInterceptor routeMatchingInterceptor) {
    this.routeMatchingInterceptor = routeMatchingInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(routeMatchingInterceptor).addPathPatterns("/**");
  }
}

