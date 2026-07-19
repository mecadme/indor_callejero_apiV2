package com.indorcallejero.api.playerstatistics;

import com.indorcallejero.api.match.InvalidMatchStateException;
import com.indorcallejero.api.match.MatchEntity;
import com.indorcallejero.api.match.MatchNotFoundException;
import com.indorcallejero.api.match.MatchRepository;
import com.indorcallejero.api.match.MatchStatus;
import com.indorcallejero.api.player.PlayerEntity;
import com.indorcallejero.api.player.PlayerNotFoundException;
import com.indorcallejero.api.player.PlayerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Transactional a nivel de clase: PlayerMatchEventDTO expone
 * playerFirstName/playerLastName (LAZY en la entidad), mismo patrón que
 * MatchService/PlayerService.
 *
 * No valida que el jugador pertenezca a alguno de los dos equipos del
 * partido -- ese cruce necesitaría el line-up (Etapa 11, gap conocido,
 * sin decisión todavía) para tener sentido de verdad. Cargar un evento de
 * un jugador ajeno al partido hoy no se rechaza, a propósito: agregar esa
 * regla ahora sería inventar una validación a medias sin la base real que
 * la sostenga.
 */
@Service
@Transactional
public class PlayerStatisticsService {

    private final PlayerMatchEventRepository eventRepository;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final PlayerMatchEventMapper eventMapper;

    public PlayerStatisticsService(
            PlayerMatchEventRepository eventRepository,
            MatchRepository matchRepository,
            PlayerRepository playerRepository,
            PlayerMatchEventMapper eventMapper
    ) {
        this.eventRepository = eventRepository;
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.eventMapper = eventMapper;
    }

    // Solo con el partido IN_PROGRESS -- mismo criterio que recordResult
    // en MatchService: cargar eventos de un partido que todavía no
    // arrancó (o que ya cerró) no tiene sentido de negocio.
    public List<PlayerMatchEventDTO> recordEvents(Long matchId, RecordPlayerEventsRequest request) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new InvalidMatchStateException(
                    "El partido " + matchId + " no está IN_PROGRESS (está en " + match.getStatus()
                            + "), no se le pueden cargar eventos");
        }

        List<PlayerMatchEventEntity> events = request.events().stream()
                .map(eventRequest -> {
                    PlayerEntity player = playerRepository.findById(eventRequest.playerId())
                            .orElseThrow(() -> new PlayerNotFoundException(eventRequest.playerId()));
                    return new PlayerMatchEventEntity(match, player, eventRequest.statType(), eventRequest.minute());
                })
                .toList();

        return eventRepository.saveAll(events).stream().map(eventMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<PlayerMatchEventDTO> getEventsByMatch(Long matchId, Pageable pageable) {
        return eventRepository.findByMatchId(matchId, pageable).map(eventMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<PlayerStatCountDTO> getStatsByPlayer(Long playerId) {
        return eventRepository.countByPlayerGroupedByStatType(playerId);
    }

    @Transactional(readOnly = true)
    public List<PlayerStatCountDTO> getStatsByMatch(Long matchId) {
        return eventRepository.countByMatchGroupedByPlayerAndStatType(matchId);
    }
}
