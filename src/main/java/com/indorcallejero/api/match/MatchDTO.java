package com.indorcallejero.api.match;

import java.time.Instant;

public record MatchDTO(
        Long id,
        Long homeTeamId,
        String homeTeamName,
        Long awayTeamId,
        String awayTeamName,
        Long roundId,
        String roundName,
        Long refereeId,
        String refereeFirstName,
        String refereeLastName,
        Instant scheduledAt,
        MatchStatus status,
        Integer goalsHomeTeam,
        Integer goalsAwayTeam
) {
}
