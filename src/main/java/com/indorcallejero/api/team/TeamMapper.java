package com.indorcallejero.api.team;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TeamMapper {

    TeamDTO toDto(TeamEntity entity);
}
