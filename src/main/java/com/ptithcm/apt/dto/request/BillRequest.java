package com.ptithcm.apt.dto.request;

import java.math.BigDecimal;

public record BillRequest(
        Long apartment,
        BigDecimal electricityFee,
        BigDecimal waterFee,
        BigDecimal managementFee,
        BigDecimal safetyFee,
        BigDecimal sanitationFee) {
}
