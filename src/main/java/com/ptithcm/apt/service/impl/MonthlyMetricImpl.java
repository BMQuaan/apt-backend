package com.ptithcm.apt.service.impl;

import java.math.BigDecimal;

import com.ptithcm.apt.dto.request.CreateMonthlyMetricRequest;
import com.ptithcm.apt.dto.response.CreateMonthlyMetricResponse;
import com.ptithcm.apt.service.MonthlyMetricService;

import jakarta.transaction.Transactional;

public class MonthlyMetricImpl implements MonthlyMetricService {

    @Override
    @Transactional
    public CreateMonthlyMetricResponse createMonthlyMetric(CreateMonthlyMetricRequest req) {
        return null;
    }

}
