package ee.ut.quickbite.userservice.dto;

import ee.ut.quickbite.userservice.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UpdateUserDTO(

        UUID userId,
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,
        String password,
        String fullName,
        String phoneNumber,
        User.Role role,
        User.Status status,
        AddressDTO address) {
}
