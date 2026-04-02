package com.ptithcm.apt.mapper;

import com.ptithcm.apt.dto.response.ServiceConfigResponse;
import com.ptithcm.apt.entity.ServiceConfig;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServiceConfigMapper {
    ServiceConfigResponse toResponse(ServiceConfig entity);
    List<ServiceConfigResponse> toResponseList(List<ServiceConfig> entities);
}
