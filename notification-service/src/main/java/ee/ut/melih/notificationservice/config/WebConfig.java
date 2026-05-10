package ee.ut.melih.notificationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Permissive CORS for the dev profile only — the API Gateway is the
 * single CORS source of truth in production. With the gateway absent
 * during local frontend dev, the browser hits this service directly,
 * so we open the relevant origins here.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOrigins(
            "http://localhost:8090",
            "http://localhost:8080",
            "http://localhost:3000")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders("Location")
        .allowCredentials(true)
        .maxAge(3600);
  }
}
