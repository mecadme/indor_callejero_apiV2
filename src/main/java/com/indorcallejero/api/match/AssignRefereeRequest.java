package com.indorcallejero.api.match;

// Sin @NotNull en refereeId: null es "desasigná el árbitro de este
// partido", una entrada válida, no un dato faltante.
public record AssignRefereeRequest(
        Long refereeId
) {
}
