package com.indorcallejero.api.ethicsofficer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EthicsOfficerRepository extends JpaRepository<EthicsOfficerEntity, Long> {

    // @EntityGraph, no @ManyToOne EAGER (PERF-02, mismo criterio que
    // PlayerRepository): el join a "team" solo pasa en las consultas que
    // arman el DTO con nombre de equipo incluido.
    @Override
    @EntityGraph(attributePaths = "team")
    Page<EthicsOfficerEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "team")
    Page<EthicsOfficerEntity> findByTeamId(Long teamId, Pageable pageable);
}
