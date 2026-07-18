package com.indorcallejero.api.match;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MatchMapper {

    @Mapping(source = "homeTeam.id", target = "homeTeamId")
    @Mapping(source = "homeTeam.name", target = "homeTeamName")
    @Mapping(source = "awayTeam.id", target = "awayTeamId")
    @Mapping(source = "awayTeam.name", target = "awayTeamName")
    @Mapping(source = "round.id", target = "roundId")
    @Mapping(source = "round.name", target = "roundName")
    @Mapping(source = "referee.id", target = "refereeId")
    @Mapping(source = "referee.firstName", target = "refereeFirstName")
    @Mapping(source = "referee.lastName", target = "refereeLastName")
    MatchDTO toDto(MatchEntity entity);
}
