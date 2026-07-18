package com.indorcallejero.api.match;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

    // Mismo criterio que PlayerRepository en la Etapa 6: LAZY por default en
    // la entidad, @EntityGraph solo en la consulta de listado que de verdad
    // arma un DTO con los nombres de ambos equipos.
    @Override
    @EntityGraph(attributePaths = {"homeTeam", "awayTeam"})
    Page<MatchEntity> findAll(Pageable pageable);
}
