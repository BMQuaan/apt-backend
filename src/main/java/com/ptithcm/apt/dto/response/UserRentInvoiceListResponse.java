package com.ptithcm.apt.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ptithcm.apt.enums.RentStatus;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserRentInvoiceListResponse(
        Long id,
        String apartmentName,
        Integer billingMonth,
        Integer billingYear,
        BigDecimal rentAmount,
        RentStatus status,
        LocalDateTime dueDate,
        String viewerRole,
        String tenantName) {
}
