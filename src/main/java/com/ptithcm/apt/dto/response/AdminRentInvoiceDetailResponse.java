package com.ptithcm.apt.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ptithcm.apt.enums.RentStatus;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminRentInvoiceDetailResponse(
                long id,
                long apartmentId,
                String apartmentName,
                String apartmentFloor,
                BigDecimal apartmentArea,
                int billingMonth,
                int billingYear,
                BigDecimal rentAmount,
                String tentnantName,
                String ownerName,
                RentStatus status,
                String createdBy,
                LocalDateTime createdAt,
                String confirmedBy,
                LocalDateTime paidAt,
                LocalDateTime dueDate) {
}
