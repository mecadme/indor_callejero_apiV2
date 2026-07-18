package com.indorcallejero.api.match;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MatchMapper {

    @Mapping(source = "homeTeam.id", target = "homeTeamId")
    @Mapping(source = "homeTeam.name", target = "homeTeamName")
    @Mapping(source = "awayTeam.id", target = "awayTeamId")
    @Mapping(source = "awayTeam.name", target = "awayTeamName")
    MatchDTO toDto(MatchEntity entity);
}
