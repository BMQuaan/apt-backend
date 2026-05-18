package com.ptithcm.apt.service;

import java.util.Optional;

import com.ptithcm.apt.dto.request.CreateMonthlyMetricRequest;
import com.ptithcm.apt.dto.response.PreviousMonthlyMetricResponse;
import com.ptithcm.apt.entity.MonthlyMetric;
import com.ptithcm.apt.dto.response.MonthlyMetricResponse;

public interface MonthlyMetricService {

    MonthlyMetricResponse createMonthlyMetric(CreateMonthlyMetricRequest req);
    PreviousMonthlyMetricResponse getPreviousMonthlyMetric(Long apartmentId);
    Optional<MonthlyMetric> findFirstByApartmentIdOrderByBillingYearDescBillingMonthDesc(Long apartmentId);
}
