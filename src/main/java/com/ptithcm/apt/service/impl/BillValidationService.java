package com.ptithcm.apt.service.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.ptithcm.apt.dto.BillValidationResult;
import com.ptithcm.apt.dto.request.CreateBillRequest;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.MonthlyMetric;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.service.ApartmentService;
import com.ptithcm.apt.service.MonthlyMetricService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BillValidationService {

        private final ApartmentService apartmentService;
        private final MonthlyMetricService monthlyMetricService;

        /**
         * Validate nghiệp vụ trước khi tạo bill.
         * Kiểm tra: apartment tồn tại, không AVAILABLE, billing period hợp lệ,
         * chỉ số điện/nước mới >= cũ.
         *
         * @param req request tạo bill
         * @return kết quả gồm apartment đã validate và chỉ số cũ
         */
        public BillValidationResult validateCreateBill(CreateBillRequest req) {
                Apartment apt = apartmentService.findById(req.apartmentId())
                                .orElseThrow(() -> new NotFoundException("Apartment not found"));

                if ("AVAILABLE".equals(apt.getStatus())) {
                        throw new RuntimeException(
                                        "Cannot create a bill for an AVAILABLE apartment.");
                }

                MonthlyMetric lastMetric = monthlyMetricService
                                .findFirstByApartmentIdOrderByBillingYearDescBillingMonthDesc(apt.getId())
                                .orElse(null);

                if (lastMetric != null) {
                        if (req.year() < lastMetric.getBillingYear() ||
                                        (req.year().equals(lastMetric.getBillingYear())
                                                        && req.month() <= lastMetric.getBillingMonth())) {
                                throw new RuntimeException(
                                                "Cannot create bill for a period that already has metrics or is in the past.");
                        }
                }

                BigDecimal oldElec = (lastMetric != null) ? lastMetric.getElectricityNew() : BigDecimal.ZERO;
                BigDecimal oldWater = (lastMetric != null) ? lastMetric.getWaterNew() : BigDecimal.ZERO;

                if (req.electricityService().compareTo(oldElec) < 0) {
                        throw new RuntimeException(
                                        "New electricity index (" + req.electricityService()
                                                        + ") cannot be less than old index (" + oldElec + ")");
                }
                if (req.waterService().compareTo(oldWater) < 0) {
                        throw new RuntimeException(
                                        "New water index (" + req.waterService()
                                                        + ") cannot be less than old index (" + oldWater + ")");
                }

                return new BillValidationResult(apt, oldElec, oldWater);
        }
}
