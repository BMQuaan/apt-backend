package com.ptithcm.apt.controller;

import com.ptithcm.apt.dto.request.ServicePriceUpdateRequest;
import com.ptithcm.apt.dto.response.AdminServiceConfigResponse;
import com.ptithcm.apt.dto.response.ApiResponse;
import com.ptithcm.apt.dto.response.ServiceConfigResponse;
import com.ptithcm.apt.service.ServiceConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/service-configs")
@RequiredArgsConstructor
public class ServiceConfigController {

    private final ServiceConfigService serviceConfigService;

    @PostMapping("/update-prices")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updatePrices(
            @Valid @RequestBody ServicePriceUpdateRequest request) {

        serviceConfigService.updateServicePrices(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật giá dịch vụ thành công"));
    }

    @GetMapping("/admin-dashboard")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminServiceConfigResponse>>> getAdminDashboardPrices() {

        List<AdminServiceConfigResponse> prices = serviceConfigService.getAdminDashboardPrices();
        return ResponseEntity.ok(ApiResponse.success(prices, "Lấy danh sách bảng giá Dashboard thành công"));
    }

    // @GetMapping("/current-prices")
    // public ResponseEntity<ApiResponse<List<ServiceConfigResponse>>>
    // getCurrentPrices() {
    //
    // List<ServiceConfigResponse> prices =
    // serviceConfigService.getPricesByDate(LocalDate.now());
    // return ResponseEntity.ok(ApiResponse.success(prices, "Lấy bảng giá hiện tại
    // thành công"));
    // }

    @DeleteMapping("/cancel-update/{serviceCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelUpdate(@PathVariable String serviceCode) {
        serviceConfigService.cancelUpcomingUpdate(serviceCode);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã hủy lịch cập nhật giá cho dịch vụ: " + serviceCode));
    }

    @GetMapping("/prices-by-date")
    public ResponseEntity<ApiResponse<List<ServiceConfigResponse>>> getPricesByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {

        List<ServiceConfigResponse> prices = serviceConfigService.getPricesByDate(targetDate);
        return ResponseEntity.ok(ApiResponse.success(prices, "Lấy bảng giá thành công cho ngày " + targetDate));
    }
}
