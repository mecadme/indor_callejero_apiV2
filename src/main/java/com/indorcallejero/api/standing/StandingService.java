package com.indorcallejero.api.standing;

import com.indorcallejero.api.team.TeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StandingService {

    private final StandingRepository standingRepository;
    private final TeamRepository teamRepository;
    private final StandingMapper standingMapper;

    public StandingService(StandingRepository standingRepository, TeamRepository teamRepository, StandingMapper standingMapper) {
        this.standingRepository = standingRepository;
        this.teamRepository = teamRepository;
        this.standingMapper = standingMapper;
    }

    @Transactional(readOnly = true)
    public Page<StandingDTO> getStandings(Pageable pageable) {
        return standingRepository.findAllByOrderByPointsDescGoalsForDesc(pageable).map(standingMapper::toDto);
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
     */
    @Transactional
    public void recordMatchResult(Long teamId, int goalsFor, int goalsAgainst) {
        StandingEntity standing = standingRepository.findByTeamId(teamId)
                .orElseGet(() -> new StandingEntity(teamRepository.getReferenceById(teamId)));
        standing.recordMatch(goalsFor, goalsAgainst);
        standingRepository.save(standing);
    }
}
