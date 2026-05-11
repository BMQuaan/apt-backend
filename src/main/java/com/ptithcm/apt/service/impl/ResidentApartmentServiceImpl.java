package com.ptithcm.apt.service.impl;

import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.service.ResidentApartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResidentApartmentServiceImpl implements ResidentApartmentService {

    private final ResidentApartmentRepository residentApartmentRepository;

    @Override
    public List<ResidentApartment> findActiveByResidentId(Long residentId) {
        return residentApartmentRepository.findByResident_IdAndIsActiveTrue(residentId);
    }

    @Override
    public List<ResidentApartment> findActiveByApartmentId(Long apartmentId) {
        return residentApartmentRepository.findByApartment_IdAndIsActiveTrue(apartmentId);
    }
}
