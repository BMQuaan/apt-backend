package com.ptithcm.apt.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import com.ptithcm.apt.dto.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ptithcm.apt.dto.request.UpdateRentInvoiceStatusRequest;
import com.ptithcm.apt.dto.response.AdminRentInvoiceDetailResponse;
import com.ptithcm.apt.dto.response.AdminRentInvoiceListResponse;
import com.ptithcm.apt.dto.response.PageResponse;
import com.ptithcm.apt.dto.response.UpdateRentInvoiceStatusResponse;
import com.ptithcm.apt.dto.response.UserRentInvoiceListResponse;
import com.ptithcm.apt.enums.RentStatus;
import com.ptithcm.apt.service.RentInvoiceService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RentInvoiceController {
    private final RentInvoiceService rentInvoiceService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/rent-invoices")
    public ResponseEntity<ApiResponse<PageResponse<AdminRentInvoiceListResponse>>> getRentInvoiceListByAdmin(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long apartmentId,
            @RequestParam(required = false) RentStatus status,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable) {
        Page<AdminRentInvoiceListResponse> rentinvoices = rentInvoiceService.getRentInvoiceListByAdmin(month, year,
                apartmentId, status, pageable);
        return ResponseEntity
                .ok(ApiResponse.success(PageResponse.from(rentinvoices), "Successfully fetched rent invoices"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/rent-invoices/{id}")
    public ResponseEntity<ApiResponse<AdminRentInvoiceDetailResponse>> getRentInvoiceDetailByAdmin(
            @PathVariable Long id) {
        AdminRentInvoiceDetailResponse res = rentInvoiceService.getRentInvoiceDetailByAdmin(id);
        return ResponseEntity.ok(ApiResponse.success(res, "Successfully fetched rent invoice detail"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/rent-invoices/{id}/status")
    public ResponseEntity<ApiResponse<UpdateRentInvoiceStatusResponse>> updateRentInvoiceStatus(
            @PathVariable Long id,
            @RequestBody UpdateRentInvoiceStatusRequest req) {
        UpdateRentInvoiceStatusResponse res = rentInvoiceService.updateRentInvoiceStatus(id, req);
        return ResponseEntity.ok(ApiResponse.success(res, "Successfully updated rent invoice status"));
    }

    @GetMapping("/me/rent-invoices")
    public ResponseEntity<ApiResponse<PageResponse<UserRentInvoiceListResponse>>> getMyRentInvoices(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long apartmentId,
            @RequestParam(required = false) RentStatus status,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable) {
        Page<UserRentInvoiceListResponse> res = rentInvoiceService.getMyRentInvoices(month, year, apartmentId, status,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(res), "Successfully fetched my rent invoices"));
    }

}
