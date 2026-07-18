package com.indorcallejero.api.player;

// teamId nullable a propósito -- mandar null desasigna al jugador de
// cualquier equipo (vuelve a la lista de "sin equipo").
public record AssignTeamRequest(Long teamId) {
}
