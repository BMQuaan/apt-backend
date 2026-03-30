package com.ptithcm.apt.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ptithcm.apt.dto.request.BillRequest;
import com.ptithcm.apt.dto.request.UpdateBillStatusRequest;
import com.ptithcm.apt.dto.response.CreateBillResponse;
import com.ptithcm.apt.dto.response.UpdateBillStatusResponse;
import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.repository.ApartmentRepository;
import com.ptithcm.apt.repository.BillRepository;
import com.ptithcm.apt.service.BillService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;
    private final ApartmentRepository apartmentRepository;

    @Override
    public CreateBillResponse createBill(BillRequest req) {
        Bill bill = Bill.builder()
                .apartment(apartmentRepository.findById(req.apartment()).get())
                .billingMonth(LocalDateTime.now().getMonthValue())
                .billingYear(LocalDateTime.now().getYear())
                .waterFee(BigDecimal.ZERO)
                .safetyFee(BigDecimal.ZERO)
                .managementFee(BigDecimal.ZERO)
                .sanitationFee(BigDecimal.ZERO)
                .electricityFee(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();
        billRepository.save(bill);

        return CreateBillResponse.builder()
                .id(bill.getId())
                .apartment(bill.getApartment().getId())
                .apartmentName(bill.getApartment().getRoomNumber())
                .billingMonth(bill.getBillingMonth())
                .billingYear(bill.getBillingYear())
                .electricityFee(bill.getElectricityFee())
                .waterFee(bill.getWaterFee())
                .managementFee(bill.getManagementFee())
                .safetyFee(bill.getSafetyFee())
                .sanitationFee(bill.getSanitationFee())
                .totalAmount(bill.getTotalAmount())
                .status(bill.getStatus())
                .createdAt(bill.getCreatedAt())
                .build();
    }

    @Override
    public UpdateBillStatusResponse updateBillStatus(Long billId, UpdateBillStatusRequest req) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        BillStatus newStatus = req.status();

        if (bill.getStatus().equals(newStatus.toString())) {
            throw new RuntimeException("Can not change to the same status");
        }

        bill.setStatus(newStatus.toString());
        if (newStatus == BillStatus.PAID) {
            bill.setPaidAt(LocalDateTime.now());
        }
        billRepository.save(bill);

        return UpdateBillStatusResponse.builder()
                .id(bill.getId())
                .apartment(bill.getApartment().getId())
                .apartmentName(bill.getApartment().getRoomNumber())
                .billingMonth(bill.getBillingMonth())
                .billingYear(bill.getBillingYear())
                .electricityFee(bill.getElectricityFee())
                .waterFee(bill.getWaterFee())
                .managementFee(bill.getManagementFee())
                .safetyFee(bill.getSafetyFee())
                .sanitationFee(bill.getSanitationFee())
                .totalAmount(bill.getTotalAmount())
                .status(bill.getStatus())
                .createdAt(bill.getCreatedAt())
                .paidAt(bill.getPaidAt())
                .build();
    }

    @Override
    public List<Bill> getBills() {
        return billRepository.findAll();
    }

}
