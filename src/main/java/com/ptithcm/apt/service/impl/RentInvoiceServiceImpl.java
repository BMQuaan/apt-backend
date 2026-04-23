package com.ptithcm.apt.service.impl;

import java.security.Security;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ptithcm.apt.dto.request.CreateRentInvoiceRequest;
import com.ptithcm.apt.dto.request.UpdateRentInvoiceStatusRequest;
import com.ptithcm.apt.dto.response.AdminRentInvoiceDetailResponse;
import com.ptithcm.apt.dto.response.AdminRentInvoiceListResponse;
import com.ptithcm.apt.dto.response.RentInvoiceResponse;
import com.ptithcm.apt.dto.response.UpdateRentInvoiceStatusReponse;
import com.ptithcm.apt.entity.RentInvoice;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.enums.RentStatus;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.mappers.RentInvoiceMapper;
import com.ptithcm.apt.repository.RentInvoiceRepository;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.repository.UserRepository;
import com.ptithcm.apt.repository.specifications.RentInvoiceSpecifications;
import com.ptithcm.apt.service.RentInvoiceService;
import com.ptithcm.apt.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RentInvoiceServiceImpl implements RentInvoiceService {
        private final RentInvoiceRepository rentInvoiceRepository;
        private final ResidentApartmentRepository residentApartmentRepository;
        private final RentInvoiceMapper rentInvoiceMapper;
        private final UserRepository userRepository;

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
                                        + contract.getResident().getFullName() + "] expired on "
                                        + contract.getContractEnd());
                }

                if (contract.getContractEnd() != null && contract.getContractEnd().isBefore(billingDate)) {
                        throw new RuntimeException(
                                        "The contract will expire before the beginning of this billing period ("
                                                        + req.month()
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

        @Override
        public Page<AdminRentInvoiceListResponse> getRentInvoiceListByAdmin(Integer month, Integer year,
                        Long apartmentId,
                        RentStatus status, Pageable pageable) {
                Specification<RentInvoice> spec = RentInvoiceSpecifications.hasFilters(month, year, apartmentId,
                                status);
                Page<RentInvoice> rentInvoices = rentInvoiceRepository.findAll(spec, pageable);

                return rentInvoices.map(rentInvoiceMapper::toGetRentInvoiceListResponse);
        }

        @Override
        public AdminRentInvoiceDetailResponse getRentInvoiceDetailByAdmin(Long id) {
                RentInvoice rentInvoice = rentInvoiceRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Rent invoice not found"));
                return rentInvoiceMapper.toGetRentInvoiceDetailResponse(rentInvoice);
        }

        @Override
        public UpdateRentInvoiceStatusReponse updateRentInvoiceStatus(Long id, UpdateRentInvoiceStatusRequest req) {
    RentInvoice rentInvoice = rentInvoiceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Rent invoice not found"));

    RentStatus currentStatus = rentInvoice.getStatus();
    RentStatus newStatus = req.status();

    if (newStatus != RentStatus.PAID) {
        throw new RuntimeException("API only supports updating status to PAID");
    }

    if (currentStatus == RentStatus.PAID) {
        throw new RuntimeException("Invoice is already PAID");
    }

    if (currentStatus != RentStatus.UNPAID && currentStatus != RentStatus.LATE) {
        throw new RuntimeException("Cannot pay invoice with current status: " + currentStatus);
    }

    rentInvoice.setStatus(RentStatus.PAID);
    rentInvoice.setPaidAt(LocalDateTime.now());

    String username = SecurityUtils.getCurrentUsername();
    User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Authenticated user " + username + " not found"));

    rentInvoice.setConfirmedBy(currentUser);

    rentInvoiceRepository.save(rentInvoice);
    return rentInvoiceMapper.toUpdateBillStatusResponse(rentInvoice);
}
}
