package com.ptithcm.apt.service;

import com.ptithcm.apt.dto.request.CreateMonthlyMetricRequest;
import com.ptithcm.apt.dto.response.PreviousMonthlyMetricResponse;
import com.ptithcm.apt.dto.response.MonthlyMetricResponse;

public interface MonthlyMetricService {

    MonthlyMetricResponse createMonthlyMetric(CreateMonthlyMetricRequest req);
    PreviousMonthlyMetricResponse getPreviousMonthlyMetric(Long apartmentId);
}
