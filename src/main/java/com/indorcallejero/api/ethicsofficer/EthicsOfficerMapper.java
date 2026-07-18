package com.indorcallejero.api.ethicsofficer;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EthicsOfficerMapper {

    @Mapping(source = "team.id", target = "teamId")
    @Mapping(source = "team.name", target = "teamName")
    EthicsOfficerDTO toDto(EthicsOfficerEntity entity);
}
