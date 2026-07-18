package com.indorcallejero.api.referee;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefereeMapper {

    RefereeDTO toDto(RefereeEntity entity);
}
