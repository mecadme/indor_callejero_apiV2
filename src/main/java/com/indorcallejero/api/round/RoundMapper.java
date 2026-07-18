package com.indorcallejero.api.round;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoundMapper {

    RoundDTO toDto(RoundEntity entity);
}
