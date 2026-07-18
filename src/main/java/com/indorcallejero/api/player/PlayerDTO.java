package com.indorcallejero.api.player;

public record PlayerDTO(
        Long id,
        String firstName,
        String lastName,
        int jerseyNumber,
        PlayerPosition position,
        int age,
        float height,
        PlayerStatus status,
        String photoUrl,
        Long teamId,
        String teamName
) {
}
