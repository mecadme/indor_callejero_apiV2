package com.indorcallejero.api.playerstatistics;

// Resultado de una agregación GROUP BY (ver PlayerMatchEventRepository) --
// no viene de mapear una entidad, así que no tiene mapper propio.
public record PlayerStatCountDTO(
        Long playerId,
        String playerFirstName,
        String playerLastName,
        StatType statType,
        Long count
) {
}
