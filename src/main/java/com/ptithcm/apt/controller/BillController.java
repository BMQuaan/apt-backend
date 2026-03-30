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

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/api/public/v1/bills")
@RequiredArgsConstructor
public class BillController {
    private final BillService billService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateBillResponse>> createBill(@RequestBody BillRequest req){       
        CreateBillResponse res = billService.createBill(req);
        return ResponseEntity.ok(ApiResponse.success(res, "Tạo hóa đơn thành công"));
    }

    @PostMapping("/update-status")
    public ResponseEntity<ApiResponse<UpdateBillStatusResponse>> updateBillStatus( @RequestParam Long billId, @RequestBody UpdateBillStatusRequest req) {
        UpdateBillStatusResponse res = billService.updateBillStatus(billId, req);
        return ResponseEntity.ok(ApiResponse.success(res, "Cập nhật trạng thái hóa đơn thành công"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Bill>>> getBills() {
        List<Bill> bills = billService.getBills();
        return ResponseEntity.ok(ApiResponse.success(bills, "Lấy danh sách hóa đơn thành công"));
    }
    
    
    
}
