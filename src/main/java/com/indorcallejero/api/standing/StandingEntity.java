package com.indorcallejero.api.standing;

import com.indorcallejero.api.team.TeamEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * PERF-03 del audit: dos partidos que terminan casi al mismo tiempo pueden
 * afectar el standing del MISMO equipo (ej. juega el sábado y el domingo
 * de la misma fecha, o -- más realista -- dos hilos async procesando el
 * mismo evento en un reintento). @Version convierte una carrera de
 * lectura-modificación-escritura silenciosa (el update que "gana" pisa al
 * otro sin que nadie se entere) en una excepción explícita
 * (ObjectOptimisticLockingFailureException) que StandingsUpdateListener
 * sabe reintentar. Sin @Version, esa pérdida de datos no se ve en ningún
 * log.
 */
@Entity
@Table(name = "standings")
public class StandingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false, unique = true)
    private TeamEntity team;

    @Column(nullable = false)
    private int gamesPlayed;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int draws;

    @Column(nullable = false)
    private int losses;

    @Column(nullable = false)
    private int goalsFor;

    @Column(nullable = false)
    private int goalsAgainst;

    @Column(nullable = false)
    private int points;

    @Version
    private Long version;

    protected StandingEntity() {
    }

    public StandingEntity(TeamEntity team) {
        this.team = team;
    }

    public void recordMatch(int goalsFor, int goalsAgainst) {
        this.gamesPlayed++;
        this.goalsFor += goalsFor;
        this.goalsAgainst += goalsAgainst;
        if (goalsFor > goalsAgainst) {
            this.wins++;
            this.points += 3;
        } else if (goalsFor == goalsAgainst) {
            this.draws++;
            this.points += 1;
        } else {
            this.losses++;
        }
    }

    public Long getId() {
        return id;
    }

    public TeamEntity getTeam() {
        return team;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getWins() {
        return wins;
    }

    public int getDraws() {
        return draws;
    }

    public int getLosses() {
        return losses;
    }

    public int getGoalsFor() {
        return goalsFor;
    }

    public int getGoalsAgainst() {
        return goalsAgainst;
    }

    public int getPoints() {
        return points;
    }
}
