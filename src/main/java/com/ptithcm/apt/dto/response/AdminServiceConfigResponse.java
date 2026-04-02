package com.ptithcm.apt.dto.response;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record AdminServiceConfigResponse(
        String serviceCode,
        String serviceName,
        String unit,

        BigDecimal currentPrice,
        LocalDate currentEffectiveFrom,

        BigDecimal upcomingPrice,
        LocalDate upcomingEffectiveFrom
) {}