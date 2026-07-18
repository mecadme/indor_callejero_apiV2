package com.indorcallejero.api.standing;

import com.indorcallejero.api.config.RestPage;
import com.indorcallejero.api.team.TeamRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StandingService {

    private static final String CACHE_NAME = "standings";

    private final StandingRepository standingRepository;
    private final TeamRepository teamRepository;
    private final StandingMapper standingMapper;

    public StandingService(StandingRepository standingRepository, TeamRepository teamRepository, StandingMapper standingMapper) {
        this.standingRepository = standingRepository;
        this.teamRepository = teamRepository;
        this.standingMapper = standingMapper;
    }

    // La clave de caché la arma Spring con los parámetros del método --
    // Pageable (PageRequest) tiene equals/hashCode correctos, así que cada
    // combinación de página/tamaño/orden cachea aparte, como corresponde.
    @Cacheable(CACHE_NAME)
    @Transactional(readOnly = true)
    public Page<StandingDTO> getStandings(Pageable pageable) {
        return new RestPage<>(standingRepository.findAllByOrderByPointsDescGoalsForDesc(pageable).map(standingMapper::toDto));
    }

    /**
     * Un método, una transacción propia -- lo llama StandingsUpdateListener
     * como bean externo (no self-invocation), así cada reintento ante un
     * conflicto de versión abre una transacción y un contexto de
     * persistencia NUEVOS. Si esto fuera una llamada interna dentro de la
     * misma clase, el proxy de Spring nunca se activaría y @Transactional
     * quedaría ignorado en silencio -- ver el comentario de proxyBeanMethods
     * en SecurityConfig, mismo problema de fondo.
     *
     * getReferenceById, no findById: para crear el standing inicial solo
     * hace falta la referencia (el FK), no traer todos los campos del
     * equipo -- Hibernate arma un proxy sin pegarle a la base.
     *
     * allEntries = true, no evictar una clave puntual: el punto de UN
     * equipo cambia el ORDEN de toda la tabla (quién queda 1º, 2º...), así
     * que cualquier página cacheada de la tabla completa quedó vieja, no
     * solo la fila de este equipo.
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void recordMatchResult(Long teamId, int goalsFor, int goalsAgainst) {
        StandingEntity standing = standingRepository.findByTeamId(teamId)
                .orElseGet(() -> new StandingEntity(teamRepository.getReferenceById(teamId)));
        standing.recordMatch(goalsFor, goalsAgainst);
        standingRepository.save(standing);
    }
}
