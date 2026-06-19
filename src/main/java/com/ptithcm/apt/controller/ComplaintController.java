package com.ptithcm.apt.controller;

import com.ptithcm.apt.dto.request.CreateComplaintRequest;
import com.ptithcm.apt.dto.request.UpdateComplaintStatusRequest;
import com.ptithcm.apt.dto.response.ApiResponse;
import com.ptithcm.apt.dto.response.ComplaintResponse;
import com.ptithcm.apt.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ComplaintResponse>> createComplaint(
            @Valid @RequestBody CreateComplaintRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                complaintService.createComplaint(request),
                "Gửi khiếu nại thành công"));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ComplaintResponse>>> getMyComplaints() {
        return ResponseEntity.ok(ApiResponse.success(
                complaintService.getMyComplaints(),
                "Lấy danh sách khiếu nại của tôi thành công"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ComplaintResponse>>> getAllComplaints() {
        return ResponseEntity.ok(ApiResponse.success(
                complaintService.getAllComplaints(),
                "Lấy danh sách khiếu nại thành công"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ComplaintResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateComplaintStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                complaintService.updateStatus(id, request),
                "Cập nhật trạng thái khiếu nại thành công"));
    }
}
