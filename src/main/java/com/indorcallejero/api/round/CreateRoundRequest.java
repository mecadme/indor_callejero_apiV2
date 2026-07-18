package com.indorcallejero.api.round;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRoundRequest(
        @NotBlank String name,
        @NotNull Integer number
) {
}
