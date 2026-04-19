package com.ptithcm.apt.controller;

import com.ptithcm.apt.dto.request.ContractRequest;
import com.ptithcm.apt.dto.response.ContractResponse;
import com.ptithcm.apt.dto.response.ResidentResponse;
import com.ptithcm.apt.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ResidentResponse> createContract(@Valid @RequestBody ContractRequest request) {
        ResidentResponse response = contractService.createContract(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ContractResponse>> getAllContracts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(contractService.getAllContracts(keyword, role, pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ContractResponse> getContractDetail(@PathVariable("id") Long contractId) {
        return ResponseEntity.ok(contractService.getContractDetail(contractId));
    }
}