package com.ptithcm.apt.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ptithcm.apt.dto.request.CreateBillRequest;
import com.ptithcm.apt.dto.request.UpdateBillStatusRequest;
import com.ptithcm.apt.dto.response.ApiResponse;
import com.ptithcm.apt.dto.response.BillSummaryResponse;
import com.ptithcm.apt.dto.response.BillResponse;
import com.ptithcm.apt.dto.response.AdminBillDetailResponse;
import com.ptithcm.apt.dto.response.AdminBillListResponse;
import com.ptithcm.apt.dto.response.UserBillDetailResponse;
import com.ptithcm.apt.dto.response.UserBillListResponse;
import com.ptithcm.apt.dto.response.UpdateBillStatusResponse;
import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.service.BillService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.ptithcm.apt.dto.response.PageResponse;

@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
public class BillController {
    private final BillService billService;

    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    @PostMapping()
    public ResponseEntity<ApiResponse<BillSummaryResponse>> createBill(
            @Valid @RequestBody CreateBillRequest req) {
        BillSummaryResponse res = billService.createBill(req);
        return ResponseEntity.ok(ApiResponse.success(res, "Tạo hóa đơn thành công"));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UpdateBillStatusResponse>> updateBillStatus(@PathVariable Long id,
            @RequestBody UpdateBillStatusRequest req) {
        UpdateBillStatusResponse res = billService.updateBillStatus(id, req);
        return ResponseEntity.ok(ApiResponse.success(res, "Cập nhật trạng thái hóa đơn thành công"));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    @GetMapping()
    public ResponseEntity<ApiResponse<PageResponse<AdminBillListResponse>>> getBillsByAdmin(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long apartmentId,
            @RequestParam(required = false) BillStatus status,
            @RequestParam(required = false) String roomNumber,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable) {
        Page<AdminBillListResponse> bills = billService.getBillsByAdmin(month, year, apartmentId, status, roomNumber,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(bills), "Lấy danh sách hóa đơn thành công"));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminBillDetailResponse>> getBillDetailByAdmin(@PathVariable Long id) {
        AdminBillDetailResponse res = billService.getBillDetailByAdmin(id);
        return ResponseEntity.ok(ApiResponse.success(res, "Lấy thông tin chi tiết hóa đơn thành công"));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN','ACCOUNTANT')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<UserBillListResponse>>> getMyBills(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long apartmentId,
            @RequestParam(required = false) BillStatus status,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) @ParameterObject Pageable pageable) {
        Page<UserBillListResponse> bills = billService.getMyBills(month, year, apartmentId, status, pageable);
        return ResponseEntity
                .ok(ApiResponse.success(PageResponse.from(bills), "Lấy danh sách hóa đơn của tôi thành công"));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN','ACCOUNTANT')")
    @GetMapping("/me/{id}")
    public ResponseEntity<ApiResponse<UserBillDetailResponse>> getMyBillDetailById(
            @PathVariable Long id) {
        UserBillDetailResponse res = billService.getMyBillDetailById(id);
        return ResponseEntity.ok(ApiResponse.success(res, "Lấy thông tin chi tiết hóa đơn thành công"));
    }

}
