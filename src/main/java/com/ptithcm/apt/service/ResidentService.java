package com.ptithcm.apt.service;

import java.util.List;

import com.ptithcm.apt.entity.Resident;

public interface ResidentService {
    public List<Resident> getAllResident();

    Resident createResident(Resident resident);

    Resident updateResident(Integer id, Resident residentDetail);

    Resident getResidentById(Integer id);
}
