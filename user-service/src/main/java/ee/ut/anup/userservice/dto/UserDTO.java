package ee.ut.anup.userservice.dto;

import ee.ut.anup.userservice.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UserDTO(
        UUID userId,
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,
        @NotBlank(message = "Password is required")
        String password,
        String fullName,
        String phoneNumber,
        User.Role role,
        User.Status status,
        AddressDTO address) {
}
