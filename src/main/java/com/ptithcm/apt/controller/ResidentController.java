package com.ptithcm.apt.controller;

import com.ptithcm.apt.dto.request.MemberRequest;
import com.ptithcm.apt.dto.request.ResidentRequest;
import com.ptithcm.apt.dto.request.UpdateResidentRequest;
import com.ptithcm.apt.dto.response.MyApartmentResponse;
import com.ptithcm.apt.dto.response.ResidentDetailResponse;
import com.ptithcm.apt.dto.response.ResidentListResponse;
import com.ptithcm.apt.dto.response.ResidentResponse;
import com.ptithcm.apt.service.ResidentService; // Inject Interface
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/residents")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentService residentService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/apartments/{roomNumber}/members")
    public ResponseEntity<ResidentResponse> addMemberToApartment(
            @PathVariable String roomNumber,
            @Valid @RequestBody MemberRequest request) {
        ResidentResponse response = residentService.addMemberToApartment(roomNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")

    @GetMapping
    public ResponseEntity<Page<ResidentListResponse>> getResidents(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(residentService.getActiveResidents(keyword, pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{residentId}/apartments/{apartmentId}/move-out")
    public ResponseEntity<String> moveOut(
            @PathVariable Long residentId,
            @PathVariable Long apartmentId) {
        residentService.moveOutResident(residentId, apartmentId);
        return ResponseEntity.ok("Đã xử lý trả phòng thành công!");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ResidentDetailResponse> updateResident(
            @PathVariable("id") Long residentId,
            @Valid @RequestBody UpdateResidentRequest request) {
        ResidentDetailResponse response = residentService.updateResident(residentId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/apartments/{apartmentId}")
    public ResponseEntity<List<ResidentListResponse>> getResidentsInApartment(@PathVariable Long apartmentId) {
        List<ResidentListResponse> responses = residentService.getResidentsByApartment(apartmentId);
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ResidentDetailResponse> getResidentDetail(@PathVariable Long id) {
        return ResponseEntity.ok(residentService.getResidentDetailById(id));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<List<MyApartmentResponse>> getMyApartments() {
        List<MyApartmentResponse> myRooms = residentService.getMyApartments();
        return ResponseEntity.ok(myRooms);
    }
}
