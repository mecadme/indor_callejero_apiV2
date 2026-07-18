package com.indorcallejero.api.match;

import com.indorcallejero.api.team.TeamEntity;
import com.indorcallejero.api.team.TeamNotFoundException;
import com.indorcallejero.api.team.TeamRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PERF-03 del audit: acá es donde se publica el evento, no donde se
 * recalculan las standings. publishEvent(...) durante esta transacción
 * solo ENCOLA el evento -- Spring lo entrega recién si la transacción
 * comete, y el listener (@Async + @TransactionalEventListener AFTER_COMMIT
 * en StandingsUpdateListener) corre en otro hilo, después de que este
 * método ya devolvió la respuesta al controller. El usuario que carga un
 * resultado no espera a que se recalculen las posiciones de dos equipos.
 */
@Service
@Transactional
public class MatchService {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final MatchMapper matchMapper;
    private final ApplicationEventPublisher eventPublisher;

    public MatchService(
            MatchRepository matchRepository,
            TeamRepository teamRepository,
            MatchMapper matchMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.matchMapper = matchMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<MatchDTO> getMatches(Pageable pageable) {
        return matchRepository.findAll(pageable).map(matchMapper::toDto);
    }

    @Transactional(readOnly = true)
    public MatchDTO getMatchById(Long id) {
        return matchMapper.toDto(findOrThrow(id));
    }

    public MatchDTO createMatch(CreateMatchRequest request) {
        TeamEntity homeTeam = teamRepository.findById(request.homeTeamId())
                .orElseThrow(() -> new TeamNotFoundException(request.homeTeamId()));
        TeamEntity awayTeam = teamRepository.findById(request.awayTeamId())
                .orElseThrow(() -> new TeamNotFoundException(request.awayTeamId()));

        MatchEntity match = new MatchEntity(homeTeam, awayTeam, request.scheduledAt());
        return matchMapper.toDto(matchRepository.save(match));
    }

    public MatchDTO startMatch(Long id) {
        MatchEntity match = findOrThrow(id);
        match.start();
        return matchMapper.toDto(matchRepository.save(match));
    }

    public MatchDTO recordResult(Long id, RecordResultRequest request) {
        MatchEntity match = findOrThrow(id);
        match.recordResult(request.goalsHomeTeam(), request.goalsAwayTeam());
        MatchEntity saved = matchRepository.save(match);

        eventPublisher.publishEvent(new MatchResultRecordedEvent(
                saved.getId(),
                saved.getHomeTeam().getId(),
                saved.getAwayTeam().getId(),
                request.goalsHomeTeam(),
                request.goalsAwayTeam()));

        return matchMapper.toDto(saved);
    }

    private MatchEntity findOrThrow(Long id) {
        return matchRepository.findById(id).orElseThrow(() -> new MatchNotFoundException(id));
    }
}
