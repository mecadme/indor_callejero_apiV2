package com.indorcallejero.api.ethicsofficer;

import jakarta.validation.constraints.NotBlank;

public record CreateEthicsOfficerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName
) {
}
