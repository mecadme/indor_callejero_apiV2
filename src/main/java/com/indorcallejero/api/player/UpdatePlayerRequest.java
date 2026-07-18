package com.indorcallejero.api.player;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdatePlayerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Min(1) @Max(99) int jerseyNumber,
        @NotNull PlayerPosition position,
        @Positive int age,
        @Positive float height,
        String photoUrl
) {
}
