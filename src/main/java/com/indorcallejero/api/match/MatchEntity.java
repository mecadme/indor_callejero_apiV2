package com.indorcallejero.api.match;

import com.indorcallejero.api.team.TeamEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

// homeTeam/awayTeam son @ManyToOne(LAZY), no el EAGER que tenía el
// original (PERF-02, mismo criterio que Etapa 6). El estado
// SCHEDULED -> IN_PROGRESS -> FINISHED vive en la propia entidad, no en
// el controller (MatchController.stopMatch tenía ~40 líneas de lógica de
// dominio metidas ahí -- ARQ-04 del audit).
@Entity
@Table(name = "matches")
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", nullable = false)
    private TeamEntity homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", nullable = false)
    private TeamEntity awayTeam;

    @Column(nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    private Integer goalsHomeTeam;

    private Integer goalsAwayTeam;

    protected MatchEntity() {
    }

    public MatchEntity(TeamEntity homeTeam, TeamEntity awayTeam, Instant scheduledAt) {
        if (homeTeam.getId() != null && homeTeam.getId().equals(awayTeam.getId())) {
            throw new IllegalArgumentException("Un equipo no puede jugar contra sí mismo");
        }
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.scheduledAt = scheduledAt;
        this.status = MatchStatus.SCHEDULED;
    }

    public void start() {
        if (status != MatchStatus.SCHEDULED) {
            throw new InvalidMatchStateException(
                    "El partido " + id + " no está en SCHEDULED (está en " + status + ")");
        }
        this.status = MatchStatus.IN_PROGRESS;
    }

    public void recordResult(int goalsHomeTeam, int goalsAwayTeam) {
        if (status != MatchStatus.IN_PROGRESS) {
            throw new InvalidMatchStateException(
                    "El partido " + id + " no está IN_PROGRESS (está en " + status + "), no se le puede cargar resultado");
        }
        this.goalsHomeTeam = goalsHomeTeam;
        this.goalsAwayTeam = goalsAwayTeam;
        this.status = MatchStatus.FINISHED;
    }

    public Long getId() {
        return id;
    }

    public TeamEntity getHomeTeam() {
        return homeTeam;
    }

    public TeamEntity getAwayTeam() {
        return awayTeam;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public Integer getGoalsHomeTeam() {
        return goalsHomeTeam;
    }

    public Integer getGoalsAwayTeam() {
        return goalsAwayTeam;
    }
}
