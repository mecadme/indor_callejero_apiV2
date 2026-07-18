package com.indorcallejero.api.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTeamRequest(
        @NotBlank String name,
        String color,
        String neighborhood,
        @NotNull TeamGroup group
) {
}
