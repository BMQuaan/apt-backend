package com.ptithcm.apt.dto;

import java.math.BigDecimal;

public record BillFeeResult(
        BigDecimal waterFee,
        BigDecimal electricityFee,
        BigDecimal managementFee,
        BigDecimal sanitationFee,
        BigDecimal totalAmount) {
}
