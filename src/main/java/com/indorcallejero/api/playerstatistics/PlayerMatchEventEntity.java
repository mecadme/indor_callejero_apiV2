package com.indorcallejero.api.playerstatistics;

import com.indorcallejero.api.match.MatchEntity;
import com.indorcallejero.api.player.PlayerEntity;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * La unidad atómica es el EVENTO individual, no un conteo agregado --
 * decisión explícita para poder mezclar dos formas de cargar datos con el
 * mismo modelo: hoy, sin reloj de partido ni alineaciones (gap conocido,
 * ver la Etapa 11), estos eventos se cargan en lote después del partido
 * vía POST /api/player-statistics/matches/{id}/events con una lista.
 * El día que exista un reloj en vivo, el mismo endpoint recibe un evento
 * por vez, en tiempo real, con "minute" completo -- no hace falta
 * rediseñar nada, solo cambia CUÁNDO llama el cliente.
 */
@Entity
@Table(name = "player_match_events")
public class PlayerMatchEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatType statType;

    // Nullable a propósito: sin reloj de partido no siempre hay un minuto
    // real para cada evento cargado en lote.
    private Integer minute;

    @Column(updatable = false)
    private Instant recordedAt;

    protected PlayerMatchEventEntity() {
    }

    public PlayerMatchEventEntity(MatchEntity match, PlayerEntity player, StatType statType, Integer minute) {
        this.match = match;
        this.player = player;
        this.statType = statType;
        this.minute = minute;
    }

    @PrePersist
    void onCreate() {
        this.recordedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public MatchEntity getMatch() {
        return match;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public StatType getStatType() {
        return statType;
    }

    public Integer getMinute() {
        return minute;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
