package com.indorcallejero.api.sponsor;

import jakarta.validation.constraints.NotBlank;

public record UpdateSponsorRequest(
        @NotBlank String name,
        String websiteUrl,
        String photoUrl
) {
}
