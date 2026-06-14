package com.ptithcm.apt.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ptithcm.apt.dto.request.ContractRequest;
import com.ptithcm.apt.dto.request.MemberRequest;
import com.ptithcm.apt.dto.request.ResidentRequest;
import com.ptithcm.apt.dto.request.UpdateResidentRequest;
import com.ptithcm.apt.dto.response.MyApartmentResponse;
import com.ptithcm.apt.dto.response.ResidentDetailResponse;
import com.ptithcm.apt.dto.response.ResidentListResponse;
import com.ptithcm.apt.dto.response.ResidentResponse;
import com.ptithcm.apt.entity.Resident;

public interface ResidentService {
    ResidentDetailResponse updateResident(Long residentId, UpdateResidentRequest request);

    List<MyApartmentResponse> getMyApartments();

    Optional<String> findNameByUserId(Long userId);

    Page<ResidentListResponse> getActiveResidents(String roomNumber, Pageable pageable);

    void moveOutResident(Long residentId, Long apartmentId);

    ResidentResponse addMemberToApartment(String roomNumber, MemberRequest request);

    List<ResidentListResponse> getResidentsByApartment(Long apartmentId);

    ResidentDetailResponse getResidentDetailById(Long residentId);

    Resident findByUserId(Long userId);

    Optional<Resident> findById(Long userId);

    Optional<ResidentResponse> checkResidentByCccd(String cccd);

}
