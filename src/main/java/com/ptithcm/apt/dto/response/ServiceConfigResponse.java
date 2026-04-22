package com.ptithcm.apt.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record ServiceConfigResponse(
                String serviceCode,
                String serviceName,
                BigDecimal unitPrice,
                String unit,
                LocalDate effectiveFrom) {
}
