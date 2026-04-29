package com.ptithcm.apt.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ptithcm.apt.enums.BillStatus;

import lombok.Builder;

@Builder
public record AdminBillDetailResponse(
        Long id,
        Long apartmentId,
        String apartmentName,
        String apartmentFloor,
        BigDecimal apartmentArea,
        Integer billingMonth,
        Integer billingYear,
        BigDecimal electricityFee,
        BigDecimal waterFee,
        BigDecimal managementFee,
        BigDecimal sanitationFee,
        BigDecimal totalAmount,
        BillStatus status,
        LocalDateTime createdAt,
        String createdBy,
        String confirmBy,
        LocalDateTime paidAt,
        LocalDateTime dueDate) {

}
