package ee.ut.quickbite.orderservice.dto.external;

import java.util.UUID;

public record UserDTO(
        UUID userId,
        String email,
        String fullName
) {
}
