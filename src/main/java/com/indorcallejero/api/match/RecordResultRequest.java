package com.indorcallejero.api.match;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecordResultRequest(
        @NotNull @Min(0) Integer goalsHomeTeam,
        @NotNull @Min(0) Integer goalsAwayTeam
) {
}
