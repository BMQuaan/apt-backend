package com.ptithcm.apt.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import com.ptithcm.apt.enums.RentStatus;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateRentInvoiceResponse(
                Long id,
                Long apartment,
                String apartmentName,
                String tenantName,
                String ownerName,
                BigDecimal rentAmount,
                RentStatus status,
                String createdBy,
                LocalDateTime createdAt) {

}
