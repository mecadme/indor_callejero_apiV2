package com.indorcallejero.api.sponsor;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SponsorMapper {

    SponsorDTO toDto(SponsorEntity entity);
}
