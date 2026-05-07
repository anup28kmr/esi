package ee.ut.anup.userservice.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressDTO(
        @NotBlank(message = "Street is required")
        String street,
        @NotBlank(message = "City is required")
        String city,
        @NotBlank(message = "Postal code is required")
        String postalCode,
        String label,
        boolean isDefault) {
}
