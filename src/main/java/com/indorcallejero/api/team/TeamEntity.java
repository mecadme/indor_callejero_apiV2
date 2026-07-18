package com.indorcallejero.api.team;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A propósito, ningún @OneToMany<Player> acá. El original tenía a Team
// como dueño de una colección EAGER de jugadores (PERF-02) -- en vez de
// mapear esa relación como colección JPA, la resolvemos componiendo dos
// consultas simples en el service (PlayerRepository.findByTeamId). Evita
// por completo la conversación de LAZY-vs-EAGER para este caso: no hay
// colección que cargar mal.
@Entity
@Table(name = "teams")
public class TeamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String color;

    private String neighborhood;

    private String logoUrl;

    // "group" es palabra reservada en SQL (GROUP BY) -- sin @Column(name=...)
    // Hibernate genera `create table teams (... group enum (...) ...)` y
    // MySQL lo rechaza con un error de sintaxis. Pasó de verdad arrancando
    // esta etapa: la tabla "teams" nunca se creó, silenciosamente, hasta
    // que se probó de verdad con datos.
    @Enumerated(EnumType.STRING)
    @Column(name = "team_group", nullable = false)
    private TeamGroup group;

    protected TeamEntity() {
    }

    public TeamEntity(String name, String color, String neighborhood, TeamGroup group) {
        this.name = name;
        this.color = color;
        this.neighborhood = neighborhood;
        this.group = group;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public TeamGroup getGroup() {
        return group;
    }

    public void setGroup(TeamGroup group) {
        this.group = group;
    }
}
