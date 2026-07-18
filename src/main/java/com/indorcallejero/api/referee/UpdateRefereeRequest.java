package com.indorcallejero.api.referee;

import jakarta.validation.constraints.NotBlank;

public record UpdateRefereeRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String licenseNumber,
        String photoUrl
) {
}
