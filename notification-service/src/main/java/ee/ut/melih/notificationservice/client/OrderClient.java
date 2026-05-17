package ee.ut.melih.notificationservice.client;

import ee.ut.melih.notificationservice.security.ServiceTokenIssuer;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls Order Service to resolve the customer id of an order. Used as
 * a fallback when an incoming Kafka event payload does not carry a
 * user id directly. The A3 §6.2 payloads list only domain ids
 * ({@code paymentId}, {@code deliveryId}, {@code orderId}), so this
 * lookup is what bridges those events to the user-scoped inbox.
 *
 * <p>Authenticates with a short-lived SERVICE token signed with the
 * shared {@code JWT_SECRET}. Order Service's {@code JwtAuthFilter}
 * accepts the token because it carries the matching issuer.
 */
@Component
public class OrderClient {

  private static final Logger log = LoggerFactory.getLogger(OrderClient.class);

  private final RestClient restClient;
  private final ServiceTokenIssuer tokenIssuer;

  public OrderClient(RestClient.Builder restClientBuilder,
                     ServiceTokenIssuer tokenIssuer,
                     @Value("${order-service.url:http://localhost:7001}") String baseUrl) {
    this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    this.tokenIssuer = tokenIssuer;
    log.info("OrderClient base-url={}", baseUrl);
  }

  public Optional<UUID> getCustomerIdForOrder(long orderId) {
    try {
      OrderSummary summary = restClient.get()
          .uri("/orders/{id}", orderId)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenIssuer.issue())
          .retrieve()
          .body(OrderSummary.class);
      if (summary == null || summary.customerId() == null) {
        log.warn("Order {} lookup returned no customerId", orderId);
        return Optional.empty();
      }
      return Optional.of(summary.customerId());
    } catch (Exception e) {
      log.warn("Order {} lookup failed: {}", orderId, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Minimal projection of Order Service's response — only the fields
   * we actually need. Unknown properties are ignored by Jackson.
   */
  public record OrderSummary(Long orderId, UUID customerId) {}
}
