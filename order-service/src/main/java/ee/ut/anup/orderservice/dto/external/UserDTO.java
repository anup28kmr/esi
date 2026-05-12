package ee.ut.anup.orderservice.dto.external;

public record UserDTO(
        Long userId,
        String email,
        String fullName
) {
}
