package com.ptithcm.apt.controller;

import com.ptithcm.apt.dto.request.ResidentRequest;
import com.ptithcm.apt.dto.request.UpdateResidentRequest;
import com.ptithcm.apt.dto.response.ResidentListResponse;
import com.ptithcm.apt.dto.response.ResidentResponse;
import com.ptithcm.apt.service.ResidentService; // Inject Interface
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/residents")
@RequiredArgsConstructor
public class ResidentController {

    // Chú ý: Dùng Interface ở đây
    private final ResidentService residentService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/with-apartment")
    public ResponseEntity<String> createResidentAndAssign(@Valid @RequestBody ResidentRequest request) {
        residentService.createResidentAndAssignApartment(request);
        return ResponseEntity.ok("Tạo cư dân, cấp tài khoản và gán phòng thành công!");
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ResidentListResponse>> getResidents(
            @RequestParam(required = false) String roomNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(residentService.getActiveResidents(roomNumber, pageable));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{residentId}/move-out")
    public ResponseEntity<String> moveOut(@PathVariable Long residentId, @RequestParam Long apartmentId) {
        residentService.moveOutResident(residentId, apartmentId);
        return ResponseEntity.ok("Đã xử lý chuyển phòng thành công!");
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ResidentResponse> updateResident(
            @PathVariable("id") Long residentId,
            @Valid @RequestBody UpdateResidentRequest request) {

        ResidentResponse response = residentService.updateResident(residentId, request);
        return ResponseEntity.ok(response);
    }

}
