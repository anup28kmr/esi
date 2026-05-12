package ee.ut.quickbite.apigateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for registering route-matching interceptors and filters.
 *
 * <p>Order matters: {@link TokenIntrospectionInterceptor} runs first so an
 * unauthenticated request never reaches the route handler. The route logger
 * runs after, observing only requests that passed auth.
 */
@Configuration
public class GatewayInterceptorConfig implements WebMvcConfigurer {

  private final TokenIntrospectionInterceptor tokenIntrospectionInterceptor;
  private final RouteMatchingInterceptor routeMatchingInterceptor;

  public GatewayInterceptorConfig(
      TokenIntrospectionInterceptor tokenIntrospectionInterceptor,
      RouteMatchingInterceptor routeMatchingInterceptor) {
    this.tokenIntrospectionInterceptor = tokenIntrospectionInterceptor;
    this.routeMatchingInterceptor = routeMatchingInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(tokenIntrospectionInterceptor)
            .addPathPatterns("/**")
            .order(0);
    registry.addInterceptor(routeMatchingInterceptor)
            .addPathPatterns("/**")
            .order(1);
  }
}
