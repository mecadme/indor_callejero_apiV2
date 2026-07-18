package com.indorcallejero.api.information;

import jakarta.validation.constraints.NotBlank;

public record UpdateInformationRequest(
        @NotBlank String title,
        @NotBlank String content,
        String photoUrl
) {
}
