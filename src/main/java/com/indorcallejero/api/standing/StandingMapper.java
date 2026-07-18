package com.indorcallejero.api.standing;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StandingMapper {

    @Mapping(source = "team.id", target = "teamId")
    @Mapping(source = "team.name", target = "teamName")
    StandingDTO toDto(StandingEntity entity);
}
