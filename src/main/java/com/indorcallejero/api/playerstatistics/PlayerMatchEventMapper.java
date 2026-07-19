package com.indorcallejero.api.playerstatistics;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlayerMatchEventMapper {

    @Mapping(source = "match.id", target = "matchId")
    @Mapping(source = "player.id", target = "playerId")
    @Mapping(source = "player.firstName", target = "playerFirstName")
    @Mapping(source = "player.lastName", target = "playerLastName")
    PlayerMatchEventDTO toDto(PlayerMatchEventEntity entity);
}
