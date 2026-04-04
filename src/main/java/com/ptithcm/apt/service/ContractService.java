package com.ptithcm.apt.service;

import com.ptithcm.apt.dto.request.ContractRequest;
import com.ptithcm.apt.dto.response.ResidentResponse;

public interface ContractService {
    ResidentResponse createContract(ContractRequest request);
}
