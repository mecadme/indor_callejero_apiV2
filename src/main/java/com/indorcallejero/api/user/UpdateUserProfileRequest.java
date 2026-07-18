package com.indorcallejero.api.user;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String bio,
        String imageUrl
) {
}
