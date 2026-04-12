package com.ptithcm.apt.service.impl;

import org.springframework.stereotype.Service;

import com.ptithcm.apt.dto.request.CreateMonthlyMetricRequest;
import com.ptithcm.apt.dto.response.CreateMonthlyMetricResponse;
import com.ptithcm.apt.entity.MonthlyMetric;
import com.ptithcm.apt.mappers.MonthlyMetricMapper;
import com.ptithcm.apt.repository.MonthlyMetricRepository;
import com.ptithcm.apt.service.MonthlyMetricService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MonthlyMetricImpl implements MonthlyMetricService {
    private final MonthlyMetricMapper monthlyMetricMapper;
    private final MonthlyMetricRepository monthlyMetricRepository;

    @Override
    @Transactional
    public CreateMonthlyMetricResponse createMonthlyMetric(CreateMonthlyMetricRequest req) {
        MonthlyMetric currentMonthlyMetric = monthlyMetricMapper.toEntity(req.apartment(), req.month(), req.year(),
                req.electricityNew(), req.waterNew(), req.electricityOld(), req.waterOld());
        monthlyMetricRepository.save(currentMonthlyMetric);
        return monthlyMetricMapper.toCreateMonthlyMetricResponse(currentMonthlyMetric);

    }

}
