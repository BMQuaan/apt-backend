package com.ptithcm.apt.dto.request;

import java.math.BigDecimal;

public record CreateMonthlyMetricRequest(
        Long apartmentId,
        Integer month,
        Integer year,
        BigDecimal electricityNew,
        BigDecimal waterNew,
        BigDecimal electricityOld,
        BigDecimal waterOld) {

}
