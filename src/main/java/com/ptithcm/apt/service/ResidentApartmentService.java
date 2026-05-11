package com.ptithcm.apt.service;

import com.ptithcm.apt.entity.ResidentApartment;

import java.util.List;

public interface ResidentApartmentService {

    List<ResidentApartment> findActiveByResidentId(Long residentId);

    List<ResidentApartment> findActiveByApartmentId(Long apartmentId);
}
