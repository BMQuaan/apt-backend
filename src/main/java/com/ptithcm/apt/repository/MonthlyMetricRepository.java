package com.ptithcm.apt.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ptithcm.apt.entity.MonthlyMetric;

@Repository
public interface MonthlyMetricRepository extends JpaRepository<MonthlyMetric, Long> {
    Optional<MonthlyMetric> findFirstByApartmentIdOrderByBillingYearDescBillingMonthDesc(Long apartmentId);
    
}
