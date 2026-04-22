package com.ptithcm.apt.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ptithcm.apt.enums.BillStatus;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BillResponse(
        Long id,
        Long apartment,
        String apartmentName,
        Integer billingMonth,
        Integer billingYear,
        BigDecimal electricityFee,
        BigDecimal waterFee,
        BigDecimal managementFee,
        BigDecimal sanitationFee,
        BigDecimal totalAmount,
        BillStatus status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime dueDate
) {
}
