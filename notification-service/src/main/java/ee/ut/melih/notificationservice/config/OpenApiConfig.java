package ee.ut.melih.notificationservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI notificationServiceOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("QuickBite Notification Service API")
                .version("0.0.1")
                .description(
                    "User-facing notifications inbox plus an internal send endpoint. "
                        + "Owner: Melih Arık. Authenticated user id is propagated via the "
                        + "X-User-Id header by the API Gateway."));
  }
}
