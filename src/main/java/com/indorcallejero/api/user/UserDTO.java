package com.indorcallejero.api.user;

import java.time.Instant;

public record UserDTO(
        Long id,
        String firstName,
        String lastName,
        String username,
        String bio,
        String imageUrl,
        Instant createdAt
) {
}
