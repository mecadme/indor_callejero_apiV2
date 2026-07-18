package com.indorcallejero.api.ethicsofficer;

import jakarta.validation.constraints.NotBlank;

public record UpdateEthicsOfficerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String photoUrl
) {
}
