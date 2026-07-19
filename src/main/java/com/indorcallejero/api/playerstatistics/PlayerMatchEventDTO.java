package com.indorcallejero.api.playerstatistics;

import java.time.Instant;

public record PlayerMatchEventDTO(
        Long id,
        Long matchId,
        Long playerId,
        String playerFirstName,
        String playerLastName,
        StatType statType,
        Integer minute,
        Instant recordedAt
) {
}
