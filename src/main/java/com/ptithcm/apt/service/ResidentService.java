package com.ptithcm.apt.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ptithcm.apt.dto.request.ContractRequest;
import com.ptithcm.apt.dto.request.MemberRequest;
import com.ptithcm.apt.dto.request.ResidentRequest;
import com.ptithcm.apt.dto.request.UpdateResidentRequest;
import com.ptithcm.apt.dto.response.ResidentDetailResponse;
import com.ptithcm.apt.dto.response.ResidentListResponse;
import com.ptithcm.apt.dto.response.ResidentResponse;

public interface ResidentService {
    ResidentDetailResponse updateResident(Long residentId, UpdateResidentRequest request);

    // ResidentResponse updateResident(Integer id, ResidentRequest request);

    Page<ResidentListResponse> getActiveResidents(String roomNumber, Pageable pageable);

    void moveOutResident(Long residentId, Long apartmentId);

    // void createResidentAndAssignApartment(ResidentRequest request);

    ResidentResponse addMemberToApartment(String roomNumber, MemberRequest request);

    List<ResidentListResponse> getResidentsByApartment(Long apartmentId);

    // ResidentResponse getResidentByName(String name);

    ResidentDetailResponse getResidentDetailById(Long residentId);

}
