package com.ptithcm.apt.dto.request;

import java.math.BigDecimal;

import com.ptithcm.apt.entity.Apartment;

public record CreateMonthlyMetricRequest(
        Apartment apartment,
        Integer month,
        Integer year,
        BigDecimal electricityNew,
        BigDecimal waterNew,
        BigDecimal electricityOld,
        BigDecimal waterOld) {

}
