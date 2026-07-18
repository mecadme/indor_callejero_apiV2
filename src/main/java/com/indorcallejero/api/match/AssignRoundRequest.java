package com.indorcallejero.api.match;

// Sin @NotNull en roundId: null es "desasigná este partido de su ronda",
// una entrada válida, no un dato faltante.
public record AssignRoundRequest(
        Long roundId
) {
}
