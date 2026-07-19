package com.indorcallejero.api.playerstatistics;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecordPlayerEventRequest(
        @NotNull Long playerId,
        @NotNull StatType statType,
        // Sin @NotNull: null es "no hay reloj todavía, no sé el minuto".
        @Min(0) Integer minute
) {
}
