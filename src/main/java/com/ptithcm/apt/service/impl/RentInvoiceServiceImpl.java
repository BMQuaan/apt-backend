package com.ptithcm.apt.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ptithcm.apt.entity.RentInvoice;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.enums.RentStatus;
import com.ptithcm.apt.repository.RentInvoiceRepository;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.service.RentInvoiceService;


import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RentInvoiceServiceImpl implements RentInvoiceService {
    private final RentInvoiceRepository rentInvoiceRepository;
    private final ResidentApartmentRepository residentApartmentRepository;

    @Override
    @Transactional
    public void createMonthlyRentInvoice(Long apartmentId, Integer month, Integer year, User creator) {
        ResidentApartment contract = residentApartmentRepository.findActiveTenant(apartmentId)
                .orElseThrow(() -> new RuntimeException("The apartment is rented but there are no active tenants!"));

        Resident owner = residentApartmentRepository.findActiveOwner(apartmentId).orElse(null);
        RentInvoice invoice = RentInvoice.builder()
                .apartment(contract.getApartment())
                .billingMonth(month)
                .billingYear(year)
                .tenant(contract.getResident())
                .owner(owner)
                .rentAmount(contract.getRentalPrice())
                .status(RentStatus.UNPAID)
                .createdBy(creator)
                .build();

        rentInvoiceRepository.save(invoice);
    }

}
