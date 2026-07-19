package com.indorcallejero.api.playerstatistics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerMatchEventRepository extends JpaRepository<PlayerMatchEventEntity, Long> {

    @EntityGraph(attributePaths = "player")
    Page<PlayerMatchEventEntity> findByMatchId(Long matchId, Pageable pageable);

    // GROUP BY en la base, no sumar en memoria después de traer todas las
    // filas -- con muchos eventos por temporada, la diferencia importa.
    @Query("""
            SELECT new com.indorcallejero.api.playerstatistics.PlayerStatCountDTO(
                p.id, p.firstName, p.lastName, e.statType, COUNT(e))
            FROM PlayerMatchEventEntity e JOIN e.player p
            WHERE p.id = :playerId
            GROUP BY p.id, p.firstName, p.lastName, e.statType
            """)
    List<PlayerStatCountDTO> countByPlayerGroupedByStatType(@Param("playerId") Long playerId);

    @Query("""
            SELECT new com.indorcallejero.api.playerstatistics.PlayerStatCountDTO(
                p.id, p.firstName, p.lastName, e.statType, COUNT(e))
            FROM PlayerMatchEventEntity e JOIN e.player p
            WHERE e.match.id = :matchId
            GROUP BY p.id, p.firstName, p.lastName, e.statType
            """)
    List<PlayerStatCountDTO> countByMatchGroupedByPlayerAndStatType(@Param("matchId") Long matchId);
}
