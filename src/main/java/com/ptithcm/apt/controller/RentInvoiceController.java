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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ptithcm.apt.dto.response.AdminRentInvoiceListResponse;
import com.ptithcm.apt.dto.response.PageResponse;
import com.ptithcm.apt.enums.RentStatus;
import com.ptithcm.apt.service.RentInvoiceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/v1")
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
        Page<AdminRentInvoiceListResponse> rentinvoices = rentInvoiceService.getRentInvoiceListByAdmin(month, year, apartmentId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(rentinvoices), "Successfully fetched rent invoices"));
    }

}
