package com.ptithcm.apt.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ptithcm.apt.dto.request.ApartmentRequest;
import com.ptithcm.apt.dto.response.ApartmentResponse;
import com.ptithcm.apt.service.ApartmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(originPatterns = "*")
@RequiredArgsConstructor
public class ApartmentController {

    private final ApartmentService apartmentService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/apartment")
    public ResponseEntity<List<ApartmentResponse>> getAllApartments() {
        return ResponseEntity.ok(apartmentService.getAllApartments());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/apartment")
    public ResponseEntity<ApartmentResponse> createApartment(@RequestBody ApartmentRequest request) {
        return ResponseEntity.ok(apartmentService.createApartment(request));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/apartment/{id}")
    public ResponseEntity<ApartmentResponse> updateApartment(@PathVariable Integer id,
            @RequestBody ApartmentRequest request) {
        return ResponseEntity.ok(apartmentService.updateApartment(id, request));
    }

    // Chi tiết admin
    // Chỉ có admin, chủ nhà, người đang thuê mới xem chi tiết 1 phòng
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/apartment/{id}")
    public ResponseEntity<ApartmentResponse> getApartmentById(@PathVariable Integer id) {
        return ResponseEntity.ok(apartmentService.getApartmentById(id));
    }

}