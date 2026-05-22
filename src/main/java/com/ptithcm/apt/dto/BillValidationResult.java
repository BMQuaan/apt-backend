package com.ptithcm.apt.dto;

import java.math.BigDecimal;

import com.ptithcm.apt.entity.Apartment;

public record BillValidationResult(
        Apartment apartment,
        BigDecimal oldElectricity,
        BigDecimal oldWater) {
}
