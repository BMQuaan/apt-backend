package com.ptithcm.apt.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ptithcm.apt.dto.request.BillRequest;
import com.ptithcm.apt.dto.request.UpdateBillStatusRequest;
import com.ptithcm.apt.dto.response.ApiResponse;
import com.ptithcm.apt.dto.response.CreateBillResponse;
import com.ptithcm.apt.dto.response.UpdateBillStatusResponse;
import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.service.BillService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



import com.ptithcm.apt.dto.response.PageResponse;

@RestController
@RequestMapping("/api/public/v1/bills")
@RequiredArgsConstructor
public class BillController {
    private final BillService billService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateBillResponse>> createBill(@Valid @RequestBody BillRequest req){       
        CreateBillResponse res = billService.createBill(req);
        return ResponseEntity.ok(ApiResponse.success(res, "Successfully created bill"));
    }

    @PostMapping("/update-status")
    public ResponseEntity<ApiResponse<UpdateBillStatusResponse>> updateBillStatus( @RequestParam Long billId, @RequestBody UpdateBillStatusRequest req) {
        UpdateBillStatusResponse res = billService.updateBillStatus(billId, req);
        return ResponseEntity.ok(ApiResponse.success(res, "Successfully updated bill status"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Bill>>> getBillsByAdmin(Pageable pageable) {
        Page<Bill> bills = billService.getBillsByAdmin(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(bills), "Successfully fetched bills"));
    }
    
    
    
}
