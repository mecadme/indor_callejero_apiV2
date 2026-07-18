package com.indorcallejero.api.match;

import com.indorcallejero.api.config.RestPage;
import com.indorcallejero.api.team.TeamEntity;
import com.indorcallejero.api.team.TeamNotFoundException;
import com.indorcallejero.api.team.TeamRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    private static final String CACHE_NAME = "matches";

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

    // "Calendario": la vista pública de partidos que casi nunca cambia
    // entre dos requests pero se consulta seguido -- el ejemplo de lectura
    // caliente que PERF-04 del audit pide cachear.
    @Cacheable(CACHE_NAME)
    @Transactional(readOnly = true)
    public Page<MatchDTO> getMatches(Pageable pageable) {
        return new RestPage<>(matchRepository.findAll(pageable).map(matchMapper::toDto));
    }

    @Transactional(readOnly = true)
    public MatchDTO getMatchById(Long id) {
        return matchMapper.toDto(findOrThrow(id));
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public MatchDTO createMatch(CreateMatchRequest request) {
        TeamEntity homeTeam = teamRepository.findById(request.homeTeamId())
                .orElseThrow(() -> new TeamNotFoundException(request.homeTeamId()));
        TeamEntity awayTeam = teamRepository.findById(request.awayTeamId())
                .orElseThrow(() -> new TeamNotFoundException(request.awayTeamId()));

        MatchEntity match = new MatchEntity(homeTeam, awayTeam, request.scheduledAt());
        return matchMapper.toDto(matchRepository.save(match));
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public MatchDTO startMatch(Long id) {
        MatchEntity match = findOrThrow(id);
        match.start();
        return matchMapper.toDto(matchRepository.save(match));
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
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
