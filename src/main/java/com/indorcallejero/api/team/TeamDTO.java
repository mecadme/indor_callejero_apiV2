package com.indorcallejero.api.team;

public record TeamDTO(
        Long id,
        String name,
        String color,
        String neighborhood,
        String logoUrl,
        TeamGroup group
) {
}
