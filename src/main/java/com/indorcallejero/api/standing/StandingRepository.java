package com.indorcallejero.api.standing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StandingRepository extends JpaRepository<StandingEntity, Long> {

    Optional<StandingEntity> findByTeamId(Long teamId);

    // Orden fijo (no negociable por el cliente vía Pageable.sort): una
    // tabla de posiciones ES el orden, no un listado que alguien reordena.
    @EntityGraph(attributePaths = "team")
    Page<StandingEntity> findAllByOrderByPointsDescGoalsForDesc(Pageable pageable);
}
