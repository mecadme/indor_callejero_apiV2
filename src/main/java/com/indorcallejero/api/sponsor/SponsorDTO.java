package com.indorcallejero.api.sponsor;

public record SponsorDTO(
        Long id,
        String name,
        String websiteUrl,
        String photoUrl
) {
}
