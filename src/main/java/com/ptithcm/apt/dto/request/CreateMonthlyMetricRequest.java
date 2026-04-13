package com.ptithcm.apt.dto.request;

import java.math.BigDecimal;

import com.ptithcm.apt.entity.Apartment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateMonthlyMetricRequest(
                @NotNull Apartment apartment,
                @Min(value = 1, message = "Month must be at least 1") @Max(value = 12, message = "Month must be at most 12") @NotNull Integer month,
                @NotNull @Positive Integer year,
                @NotNull @Positive BigDecimal electricityNew,
                @NotNull @Positive BigDecimal waterNew,
                @NotNull @Positive BigDecimal electricityOld,
                @NotNull @Positive BigDecimal waterOld) {

}
