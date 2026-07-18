package com.indorcallejero.api.information;

import jakarta.validation.constraints.NotBlank;

public record CreateInformationRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}
