package ee.ut.anup.userservice.dto;

public record LoginResponseDTO(String token, UserDTO user) {
}
