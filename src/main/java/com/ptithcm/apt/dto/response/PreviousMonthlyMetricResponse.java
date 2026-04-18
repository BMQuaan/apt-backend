package com.ptithcm.apt.dto.response;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record PreviousMonthlyMetricResponse(
                Long apartmentId,
                BigDecimal latestElectricity,
                BigDecimal latestWater) {

}
