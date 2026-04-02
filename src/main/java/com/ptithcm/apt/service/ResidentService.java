package com.ptithcm.apt.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ptithcm.apt.dto.request.ResidentRequest;
import com.ptithcm.apt.dto.request.UpdateResidentRequest;
import com.ptithcm.apt.dto.response.ResidentListResponse;
import com.ptithcm.apt.dto.response.ResidentResponse;

public interface ResidentService {
    ResidentResponse updateResident(Long residentId, UpdateResidentRequest request);

    // ResidentResponse updateResident(Integer id, ResidentRequest request);

    Page<ResidentListResponse> getActiveResidents(String roomNumber, Pageable pageable);

    void moveOutResident(Long residentId, Long apartmentId);

    void createResidentAndAssignApartment(ResidentRequest request);

}
