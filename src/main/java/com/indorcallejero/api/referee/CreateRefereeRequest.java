package com.indorcallejero.api.referee;

import jakarta.validation.constraints.NotBlank;

public record CreateRefereeRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String licenseNumber
) {
}
