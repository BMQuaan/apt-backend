package com.ptithcm.apt.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ptithcm.apt.dto.request.ApartmentRequest;
import com.ptithcm.apt.dto.response.ApartmentResponse;
import com.ptithcm.apt.service.ApartmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/apartments")
@CrossOrigin(originPatterns = "*")
@RequiredArgsConstructor
public class ApartmentController {

    private final ApartmentService apartmentService;

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping
    public ResponseEntity<Page<ApartmentResponse>> getAllApartments(@RequestParam(defaultValue = "0") int page) {
        Page<ApartmentResponse> responses = apartmentService.getAllApartments(page);
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping
    public ResponseEntity<ApartmentResponse> createApartment(@Valid @RequestBody ApartmentRequest request) {
        return ResponseEntity.ok(apartmentService.createApartment(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<ApartmentResponse> updateApartment(@PathVariable Long id,
            @Valid @RequestBody ApartmentRequest request) {
        return ResponseEntity.ok(apartmentService.updateApartment(id, request));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/search")
    public ResponseEntity<List<ApartmentResponse>> searchApartmentsByRoomNumber(@RequestParam String keyword) {
        return ResponseEntity.ok(apartmentService.searchApartmentsByRoomNumber(keyword));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ApartmentResponse>> getApartmentsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(apartmentService.getApartmentsByStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApartmentResponse> getApartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(apartmentService.getApartmentById(id));
    }

}