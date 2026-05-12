package ee.ut.anup.orderservice.client;

import ee.ut.anup.orderservice.dto.external.UserDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(RestClient.Builder restClientBuilder, @Value("${user-service.url:http://localhost:7000}") String userServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(userServiceUrl).build();
    }

    public Optional<UserDTO> getUserById(Long userId) {
        try {
            UserDTO user = restClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .body(UserDTO.class);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
