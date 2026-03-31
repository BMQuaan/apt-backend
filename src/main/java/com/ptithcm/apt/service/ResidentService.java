package com.ptithcm.apt.service;

import java.util.List;

import com.ptithcm.apt.dto.request.ResidentRequest;
import com.ptithcm.apt.dto.response.ResidentResponse;

public interface ResidentService {
    List<ResidentResponse> getAllResidents();

    ResidentResponse getResidentById(Integer id);

    ResidentResponse createResident(ResidentRequest request);

    ResidentResponse updateResident(Integer id, ResidentRequest request);
}
