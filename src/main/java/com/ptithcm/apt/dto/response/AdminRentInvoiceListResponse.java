package com.ptithcm.apt.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminRentInvoiceListResponse(
                Long id,
                String apartmentName,
                Integer billingMonth,
                Integer billingYear,
                BigDecimal rentAmount,
                String status,
                LocalDateTime dueDate) {

}
