package com.indorcallejero.api.facebookvideo;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FacebookVideoMapper {

    FacebookVideoDTO toDto(FacebookVideoEntity entity);
}
