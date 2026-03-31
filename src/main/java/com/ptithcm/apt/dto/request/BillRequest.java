package com.ptithcm.apt.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BillRequest(
        @NotNull Long apartment,
        @NotNull @Positive BigDecimal electricityFee,
        @NotNull @Positive BigDecimal waterFee,
        @NotNull @Positive BigDecimal managementFee,
        @NotNull @Positive BigDecimal safetyFee,
        @NotNull @Positive BigDecimal sanitationFee) {
}
