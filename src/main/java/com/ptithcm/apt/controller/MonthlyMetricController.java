package com.ptithcm.apt.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ptithcm.apt.dto.response.ApiResponse;
import com.ptithcm.apt.dto.response.PreviousMonthlyMetricResponse;
import com.ptithcm.apt.service.MonthlyMetricService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MonthlyMetricController {
    private final MonthlyMetricService monthlyMetricService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/monthly-metrics")
    public ResponseEntity<ApiResponse<PreviousMonthlyMetricResponse>> getPreviousMonthlyMetric(
            @RequestParam("apartmentId") Long apartmentId) {
        PreviousMonthlyMetricResponse res = monthlyMetricService.getPreviousMonthlyMetric(apartmentId);
        return ResponseEntity.ok(ApiResponse.success(res, "Lấy chỉ số điện nước tháng trước thành công"));
    }

}
