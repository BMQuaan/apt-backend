package com.ptithcm.apt.service;

import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;

import java.util.List;
import java.util.Optional;

public interface ResidentApartmentService {

    List<ResidentApartment> findActiveByResidentId(Long residentId);

    List<ResidentApartment> findActiveByApartmentId(Long apartmentId);

    Optional<ResidentApartment> findActiveTenant(Long aptId);

     Optional<Resident> findActiveOwner(Long aptId);
}
