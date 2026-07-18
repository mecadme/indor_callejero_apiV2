package com.indorcallejero.api.information;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InformationMapper {

    InformationDTO toDto(InformationEntity entity);
}
