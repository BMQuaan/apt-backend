package com.ptithcm.apt.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BillSummaryResponse(
        BillResponse bill,
        RentInvoiceResponse rentInvoice,
        MonthlyMetricResponse monthlyMetric) {

}
