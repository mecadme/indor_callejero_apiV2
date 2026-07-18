package com.indorcallejero.api.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTeamRequest(
        @NotBlank String name,
        String color,
        String neighborhood,
        String logoUrl,
        @NotNull TeamGroup group
) {
}
