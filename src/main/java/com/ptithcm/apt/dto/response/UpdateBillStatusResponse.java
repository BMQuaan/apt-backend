package com.ptithcm.apt.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ptithcm.apt.enums.BillStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateBillStatusResponse {
    private Long id;
    private Long apartment;
    private String apartmentName;
    private Integer billingMonth;
    private Integer billingYear;
    private BigDecimal electricityFee;
    private BigDecimal waterFee;
    private BigDecimal managementFee;
    private BigDecimal sanitationFee;
    private BigDecimal totalAmount;
    private BillStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
