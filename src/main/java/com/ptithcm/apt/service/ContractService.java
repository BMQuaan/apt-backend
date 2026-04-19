package com.ptithcm.apt.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ptithcm.apt.dto.request.ContractRequest;
import com.ptithcm.apt.dto.response.ContractResponse;
import com.ptithcm.apt.dto.response.ResidentResponse;

public interface ContractService {
    ResidentResponse createContract(ContractRequest request);

    Page<ContractResponse> getAllContracts(String keyword, String role, Pageable pageable);

    ContractResponse getContractDetail(Long contractId);

}
