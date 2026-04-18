package com.ptithcm.apt.service.impl;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ptithcm.apt.dto.request.CreateRentInvoiceRequest;
import com.ptithcm.apt.dto.response.RentInvoiceResponse;
import com.ptithcm.apt.entity.RentInvoice;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.enums.RentStatus;
import com.ptithcm.apt.mappers.RentInvoiceMapper;
import com.ptithcm.apt.repository.RentInvoiceRepository;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.service.RentInvoiceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RentInvoiceServiceImpl implements RentInvoiceService {
    private final RentInvoiceRepository rentInvoiceRepository;
    private final ResidentApartmentRepository residentApartmentRepository;
    private final RentInvoiceMapper rentInvoiceMapper;

    @Override
    @Transactional
    public RentInvoiceResponse createMonthlyRentInvoice(CreateRentInvoiceRequest req) {
        ResidentApartment contract = residentApartmentRepository.findActiveTenant(req.apartmentId())
                .orElseThrow(() -> new RuntimeException(
                        "Apartment is marked as RENTED but no active tenant contract was found!"));

        LocalDate now = LocalDate.now();
        LocalDate billingDate = LocalDate.of(req.year(), req.month(), 1);

        if (contract.getContractEnd() != null && contract.getContractEnd().isBefore(now)) {
            throw new RuntimeException("Cannot generate invoice: The contract for resident ["
                    + contract.getResident().getFullName() + "] expired on " + contract.getContractEnd());
        }

        if (contract.getContractEnd() != null && contract.getContractEnd().isBefore(billingDate)) {
            throw new RuntimeException(
                    "The contract will expire before the beginning of this billing period (" + req.month()
                            + "/" + req.year() + ").");
        }

        Resident owner = residentApartmentRepository.findActiveOwner(req.apartmentId()).orElse(null);
        RentInvoice invoice = RentInvoice.builder()
                .apartment(contract.getApartment())
                .billingMonth(req.month())
                .billingYear(req.year())
                .tenant(contract.getResident())
                .owner(owner)
                .rentAmount(contract.getRentalPrice())
                .status(RentStatus.UNPAID)
                .createdBy(req.creator())
                .build();

        rentInvoiceRepository.save(invoice);

        return rentInvoiceMapper.toCreateRentInvoiceResponse(invoice);
    }

}
