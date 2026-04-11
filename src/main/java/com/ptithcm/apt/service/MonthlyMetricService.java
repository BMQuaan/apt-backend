package com.ptithcm.apt.service;

import java.math.BigDecimal;

import com.ptithcm.apt.dto.request.CreateMonthlyMetricRequest;
import com.ptithcm.apt.dto.response.CreateMonthlyMetricResponse;

public interface MonthlyMetricService {

    CreateMonthlyMetricResponse createMonthlyMetric(CreateMonthlyMetricRequest req);
}
