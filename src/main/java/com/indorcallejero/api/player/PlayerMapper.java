package com.indorcallejero.api.player;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlayerMapper {

    // MapStruct genera el null-check de "team" solo (jugador sin equipo
    // todavía) -- no hace falta escribirlo a mano acá.
    @Mapping(source = "team.id", target = "teamId")
    @Mapping(source = "team.name", target = "teamName")
    PlayerDTO toDto(PlayerEntity entity);
}
