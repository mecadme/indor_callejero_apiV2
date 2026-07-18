package com.indorcallejero.api.referee;

public record RefereeDTO(
        Long id,
        String firstName,
        String lastName,
        String licenseNumber,
        String photoUrl
) {
}
