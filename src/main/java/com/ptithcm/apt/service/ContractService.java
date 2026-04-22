package com.ptithcm.apt.service;

<<<<<<< HEAD
import com.ptithcm.apt.dto.request.ContractRequest;
=======
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ptithcm.apt.dto.request.ContractRequest;
import com.ptithcm.apt.dto.response.ContractResponse;
>>>>>>> main
import com.ptithcm.apt.dto.response.ResidentResponse;

public interface ContractService {
    ResidentResponse createContract(ContractRequest request);
<<<<<<< HEAD
=======

    Page<ContractResponse> getAllContracts(String keyword, String role, Pageable pageable);

    ContractResponse getContractDetail(Long contractId);

>>>>>>> main
}
