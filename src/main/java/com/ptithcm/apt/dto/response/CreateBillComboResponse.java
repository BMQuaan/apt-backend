package com.ptithcm.apt.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateBillComboResponse(
                CreateBillResponse bill,
                CreateRentInvoiceResponse rentInvoice,
                CreateMonthlyMetricResponse monthlyMetric) {

}
