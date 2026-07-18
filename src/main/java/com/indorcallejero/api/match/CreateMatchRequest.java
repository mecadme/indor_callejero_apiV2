package com.indorcallejero.api.match;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateMatchRequest(
        @NotNull Long homeTeamId,
        @NotNull Long awayTeamId,
        @NotNull @Future Instant scheduledAt
) {
}
