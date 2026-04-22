package com.ptithcm.apt.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MonthlyMetricResponse(
        Long id,
        Long apartmentId,
        String apartmentName,
        Integer billingMonth,
        Integer billingYear,
        BigDecimal electricityNew,
        BigDecimal waterNew,
        LocalDateTime createdAt) {
}
