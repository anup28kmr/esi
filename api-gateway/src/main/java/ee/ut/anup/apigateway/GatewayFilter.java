package ee.ut.anup.apigateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class GatewayFilter implements WebFilter {

    private final WebClient webClient;
    private final String userServiceUrl;
    private final String orderServiceUrl;

    public GatewayFilter(@Value("${USER_SERVICE_HOST:localhost}") String userServiceHost,
                         @Value("${USER_SERVICE_PORT:7000}") String userServicePort,
                         @Value("${ORDER_SERVICE_HOST:localhost}") String orderServiceHost,
                         @Value("${ORDER_SERVICE_PORT:7001}") String orderServicePort) {
        this.webClient = WebClient.builder().build();
        this.userServiceUrl = "http://" + userServiceHost + ":" + userServicePort;
        this.orderServiceUrl = "http://" + orderServiceHost + ":" + orderServicePort;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        String targetBase;
        if (path.startsWith("/auth") || path.startsWith("/users") || path.startsWith("/driver-profiles")) {
            targetBase = userServiceUrl;
        } else if (path.startsWith("/orders")) {
            targetBase = orderServiceUrl;
        } else {
            return chain.filter(exchange);
        }

        String query = exchange.getRequest().getURI().getRawQuery();
        String targetUrl = targetBase + path + (query != null ? "?" + query : "");

        return webClient
                .method(exchange.getRequest().getMethod())
                .uri(URI.create(targetUrl))
                .headers(h -> h.addAll(exchange.getRequest().getHeaders()))
                .body(exchange.getRequest().getBody(), DataBuffer.class)
                .exchangeToMono(response -> {
                    ServerHttpResponse clientResponse = exchange.getResponse();
                    clientResponse.setStatusCode(response.statusCode());
                    clientResponse.getHeaders().putAll(response.headers().asHttpHeaders());
                    return clientResponse.writeWith(response.bodyToFlux(DataBuffer.class));
                });
    }
}
