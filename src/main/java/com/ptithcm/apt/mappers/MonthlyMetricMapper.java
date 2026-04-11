package com.ptithcm.apt.mappers;

import java.math.BigDecimal;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ptithcm.apt.dto.request.BillRequest;
import com.ptithcm.apt.dto.response.CreateMonthlyMetricResponse;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.MonthlyMetric;

@Mapper(componentModel = "spring")
public interface MonthlyMetricMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "apartment", source = "apartment")
    @Mapping(target = "billingMonth", source = "request.month")
    @Mapping(target = "billingYear", source = "request.year")
    @Mapping(target = "electricityOld", source = "oldElec")
    @Mapping(target = "electricityNew", source = "request.electricityService")
    @Mapping(target = "waterOld", source = "oldWater")
    @Mapping(target = "waterNew", source = "request.waterService")
    @Mapping(target = "createdAt", ignore = true)
    MonthlyMetric toEntity(BillRequest request, Apartment apartment, BigDecimal oldElec, BigDecimal oldWater);

    @Mapping(source = "apartment.roomNumber", target = "apartmentName")
    @Mapping(source = "apartment.id", target = "apartmentId")
    CreateMonthlyMetricResponse toCreateRentInvoiceResponse(MonthlyMetric monthlyMetric);
}
