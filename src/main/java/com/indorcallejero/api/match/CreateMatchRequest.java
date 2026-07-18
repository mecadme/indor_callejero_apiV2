package com.indorcallejero.api.match;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateMatchRequest(
        @NotNull Long homeTeamId,
        @NotNull Long awayTeamId,
        @NotNull @Future Instant scheduledAt,
        // Opcional a propósito: un partido puede existir antes de que se
        // sepa a qué fecha/fase pertenece -- ver PATCH /api/matches/{id}/round
        // para asignarlo (o reasignarlo) después.
        Long roundId,
        // Mismo criterio: el árbitro se puede designar después, ver
        // PATCH /api/matches/{id}/referee.
        Long refereeId
) {
}
