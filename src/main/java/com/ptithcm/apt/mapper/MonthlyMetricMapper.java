package com.ptithcm.apt.mapper;

import java.math.BigDecimal;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ptithcm.apt.dto.response.MonthlyMetricResponse;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.MonthlyMetric;

@Mapper(componentModel = "spring")
public interface MonthlyMetricMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "apartment", source = "apartment")
    @Mapping(target = "billingMonth", source = "month")
    @Mapping(target = "billingYear", source = "year")
    @Mapping(target = "electricityOld", source = "oldElec")
    @Mapping(target = "electricityNew", source = "newElec")
    @Mapping(target = "waterOld", source = "oldWater")
    @Mapping(target = "waterNew", source = "newWater")
    @Mapping(target = "createdAt", ignore = true)
    MonthlyMetric toEntity(Apartment apartment, Integer month, Integer year, BigDecimal newElec, BigDecimal newWater,
            BigDecimal oldElec, BigDecimal oldWater);

    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    @Mapping(source = "apartment.id", target = "apartmentId")
    MonthlyMetricResponse toCreateMonthlyMetricResponse(MonthlyMetric monthlyMetric);
}
