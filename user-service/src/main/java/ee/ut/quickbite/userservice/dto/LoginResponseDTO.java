package ee.ut.quickbite.userservice.dto;

public record LoginResponseDTO(String token, UserDTO user) {
}
