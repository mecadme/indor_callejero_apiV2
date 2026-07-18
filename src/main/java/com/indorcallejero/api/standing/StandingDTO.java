package com.indorcallejero.api.standing;

public record StandingDTO(
        Long teamId,
        String teamName,
        int gamesPlayed,
        int wins,
        int draws,
        int losses,
        int goalsFor,
        int goalsAgainst,
        int points
) {
}
