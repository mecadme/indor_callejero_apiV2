package com.indorcallejero.api.user;

public record UpdateUserProfileRequest(
        String firstName,
        String lastName,
        String bio,
        String imageUrl
) {
}
