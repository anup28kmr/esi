package ee.ut.quickbite.apigateway.config;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.rewritePath;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Spring Cloud Gateway MVC route definitions.
 *
 * <p>Order matters: routes are evaluated top-down via {@link RouterFunction#and},
 * so more specific predicates must be composed first. In particular,
 * {@code /api/restaurants/&#123;id&#125;/menu-items} must be matched by the
 * menu-service route before the general {@code /api/restaurants/**} catch-all
 * sends it to restaurant-service.
 *
 * <p>Each route strips the {@code /api} prefix with a single shared filter so
 * downstream services receive their native paths
 * (e.g. {@code /restaurants/{id}/menu-items} on menu-service).
 */
@Configuration
public class GatewayRoutesConfig {

  private static final String STRIP_API_REGEX = "/api/(?<seg>.*)";
  private static final String STRIP_API_REPL = "/${seg}";

  @Value("${USER_SERVICE_HOST:user-service}")       private String userHost;
  @Value("${USER_SERVICE_PORT:7000}")               private int userPort;
  @Value("${ORDER_SERVICE_HOST:order-service}")     private String orderHost;
  @Value("${ORDER_SERVICE_PORT:7001}")              private int orderPort;
  @Value("${RESTAURANT_SERVICE_HOST:restaurant-service}") private String restaurantHost;
  @Value("${RESTAURANT_SERVICE_PORT:8081}")         private int restaurantPort;
  @Value("${MENU_SERVICE_HOST:menu-service}")       private String menuHost;
  @Value("${MENU_SERVICE_PORT:8082}")               private int menuPort;
  @Value("${PAYMENT_SERVICE_HOST:payment-service}") private String paymentHost;
  @Value("${PAYMENT_SERVICE_PORT:8085}")            private int paymentPort;
  @Value("${DELIVERY_SERVICE_HOST:delivery-service}") private String deliveryHost;
  @Value("${DELIVERY_SERVICE_PORT:8086}")           private int deliveryPort;

  @Bean
  public RouterFunction<ServerResponse> gatewayRoutes() {
    // The menu-via-restaurant route MUST come first. The general
    // /api/restaurants/** route would otherwise also match
    // /api/restaurants/{id}/menu-items and forward to restaurant-service,
    // which has no such endpoint.
    return apiRoute("menu-via-restaurant",
            path("/api/restaurants/{id}/menu-items"),
            menuUrl())
        .and(apiRoute("restaurant-service",
            path("/api/restaurants").or(path("/api/restaurants/**")),
            restaurantUrl()))
        .and(apiRoute("menu-service",
            path("/api/menu-items").or(path("/api/menu-items/**")),
            menuUrl()))
        .and(apiRoute("order-service",
            path("/api/orders").or(path("/api/orders/**")),
            orderUrl()))
        .and(apiRoute("user-service-auth",
            path("/api/auth").or(path("/api/auth/**")),
            userUrl()))
        .and(apiRoute("user-service-users",
            path("/api/users").or(path("/api/users/**")),
            userUrl()))
        .and(apiRoute("payment-service",
            path("/api/payments").or(path("/api/payments/**")),
            paymentUrl()))
        .and(apiRoute("delivery-service",
            path("/api/deliveries").or(path("/api/deliveries/**")),
            deliveryUrl()));
  }

  private RouterFunction<ServerResponse> apiRoute(
      String id, RequestPredicate predicate, String targetUrl) {
    // HandlerFunctions.http() (no args) reads the target URI from a request
    // attribute set by BeforeFilterFunctions.uri(...). The rewritePath filter
    // strips the /api prefix so downstream services receive their native
    // paths. Both `before(...)` filters run before the handler.
    return route(id)
        .route(predicate, http())
        .before(uri(targetUrl))
        .before(rewritePath(STRIP_API_REGEX, STRIP_API_REPL))
        .build();
  }

  private String userUrl()       { return "http://" + userHost       + ":" + userPort; }
  private String orderUrl()      { return "http://" + orderHost      + ":" + orderPort; }
  private String restaurantUrl() { return "http://" + restaurantHost + ":" + restaurantPort; }
  private String menuUrl()       { return "http://" + menuHost       + ":" + menuPort; }
  private String paymentUrl()    { return "http://" + paymentHost    + ":" + paymentPort; }
  private String deliveryUrl()   { return "http://" + deliveryHost   + ":" + deliveryPort; }
}
