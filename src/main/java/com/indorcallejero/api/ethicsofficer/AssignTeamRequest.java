package com.indorcallejero.api.ethicsofficer;

// teamId nullable a propósito -- mandar null desasigna al oficial de
// cualquier equipo.
public record AssignTeamRequest(Long teamId) {
}
