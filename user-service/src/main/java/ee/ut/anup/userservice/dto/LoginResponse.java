package ee.ut.anup.userservice.dto;

public record LoginResponse(String token, UserDTO user) {
}
