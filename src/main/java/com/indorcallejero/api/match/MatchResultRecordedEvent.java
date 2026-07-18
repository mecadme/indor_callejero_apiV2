package com.indorcallejero.api.match;

// Evento de dominio, no un ApplicationEvent con herencia como el original
// (UpdateStandingsEvent extendía ApplicationEvent solo para llevar un
// matchId). Un record alcanza: Spring publica cualquier objeto como
// evento desde 4.2, no hace falta la herencia.
public record MatchResultRecordedEvent(
        Long matchId,
        Long homeTeamId,
        Long awayTeamId,
        int goalsHomeTeam,
        int goalsAwayTeam
) {
}
