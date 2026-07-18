package com.indorcallejero.api.sponsor;

import jakarta.validation.constraints.NotBlank;

public record CreateSponsorRequest(
        @NotBlank String name,
        String websiteUrl
) {
}
