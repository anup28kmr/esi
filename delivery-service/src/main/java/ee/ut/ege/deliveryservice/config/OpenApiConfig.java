package ee.ut.ege.deliveryservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI deliveryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Delivery Service API")
                        .description("QuickBite Delivery Service — schedules and tracks deliveries for paid orders")
                        .version("1.0.0"));
    }
}
