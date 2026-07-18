package com.indorcallejero.api.player;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {

    // @EntityGraph acá, no un @ManyToOne EAGER en la entidad (PERF-02): el
    // join extra a "team" solo pasa en las consultas que de verdad lo
    // necesitan -- listar jugadores y armarles el DTO, que muestra el
    // nombre del equipo. Sin esto, cada player.getTeam().getName() de la
    // página dispara su propio SELECT (N+1: 20 jugadores, 21 queries).
    @Override
    @EntityGraph(attributePaths = "team")
    Page<PlayerEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "team")
    Page<PlayerEntity> findByTeamIsNull(Pageable pageable);

    @EntityGraph(attributePaths = "team")
    Page<PlayerEntity> findByTeamId(Long teamId, Pageable pageable);
}
