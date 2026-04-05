package com.ptithcm.apt.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ptithcm.apt.dto.request.BillRequest;
import com.ptithcm.apt.dto.request.UpdateBillStatusRequest;
import com.ptithcm.apt.dto.response.CreateBillResponse;
import com.ptithcm.apt.dto.response.GetBillsByAdminResponse;
import com.ptithcm.apt.dto.response.GetMyBillDetailByIdResponse;
import com.ptithcm.apt.dto.response.GetMyBillsResponse;
import com.ptithcm.apt.dto.response.UpdateBillStatusResponse;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.mappers.BillMapper;
import com.ptithcm.apt.repository.ApartmentRepository;
import com.ptithcm.apt.repository.BillRepository;
import com.ptithcm.apt.repository.UserRepository;
import com.ptithcm.apt.repository.specifications.BillSpecifications;
import com.ptithcm.apt.service.BillService;
import com.ptithcm.apt.service.RentInvoiceService;
import com.ptithcm.apt.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

        private final BillRepository billRepository;
        private final ApartmentRepository apartmentRepository;
        private final UserRepository userRepository;
        private final BillMapper billMapper;
        private final RentInvoiceService rentInvoiceService;

        @Override
        @Transactional
        public CreateBillResponse createBill(BillRequest req) {
                BigDecimal totalAmount = req.waterFee()
                                .add(req.managementFee())
                                .add(req.sanitationFee())
                                .add(req.electricityFee());

                Apartment apt = apartmentRepository.findById(req.apartment())
                                .orElseThrow(() -> new RuntimeException("Apartment not found"));
                String userName = SecurityUtils.getCurrentUsername();
                User currentUser = userRepository.findByUsername(userName)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Bill bill = Bill.builder()
                                .apartment(apt)
                                .billingMonth(LocalDateTime.now().getMonthValue())
                                .billingYear(LocalDateTime.now().getYear())
                                .waterFee(req.waterFee())
                                .managementFee(req.managementFee())
                                .sanitationFee(req.sanitationFee())
                                .electricityFee(req.electricityFee())
                                .totalAmount(totalAmount)
                                .createdBy(currentUser)
                                .status(BillStatus.UNPAID)
                                .build();
                billRepository.save(bill);

                if ("RENTED".equals(apt.getStatus())) {
                        rentInvoiceService.createMonthlyRentInvoice(
                                        apt.getId(),
                                        bill.getBillingMonth(),
                                        bill.getBillingYear(),
                                        currentUser);
                }

                return billMapper.toCreateBillResponse(bill);
        }

        @Override
        public UpdateBillStatusResponse updateBillStatus(Long billId, UpdateBillStatusRequest req) {
                Bill bill = billRepository.findById(billId)
                                .orElseThrow(() -> new RuntimeException("Bill not found"));

                BillStatus newStatus = req.status();

                if (bill.getStatus().equals(newStatus)) {
                        throw new RuntimeException("Can not change to the same status");
                }

                bill.setStatus(newStatus);
                if (newStatus == BillStatus.PAID) {
                        bill.setPaidAt(LocalDateTime.now());
                }

                bill.setConfirmedBy(userRepository.findByUsername(SecurityUtils.getCurrentUsername())
                                .orElseThrow(() -> new RuntimeException("User not found")));

                bill.setPaidAt(LocalDateTime.now());

                billRepository.save(bill);

                return billMapper.toUpdateBillStatusResponse(bill);
        }

        @Override
        public Page<GetBillsByAdminResponse> getBillsByAdmin(Integer month, Integer year, Long apartmentId,
                        BillStatus status, Pageable pageable) {
                Specification<Bill> spec = BillSpecifications.hasFilters(month, year, apartmentId, status);
                Page<Bill> bills = billRepository.findAll(spec, pageable);
                return bills.map(billMapper::toGetBillsByAdminResponse);
        }

        @Override
        public Page<GetMyBillsResponse> getMyBills(Integer month, Integer year, Long apartmentId, BillStatus status,
                        Pageable pageable) {
                String userName = SecurityUtils.getCurrentUsername();
                User currentUser = userRepository.findByUsername(userName)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                Long currentUserId = currentUser.getId();
                Page<Bill> bills = billRepository.findMyBills(currentUserId, apartmentId, month, year, status,
                                pageable);
                return bills.map(billMapper::toGetMyBillsResponse);
        }

        @Override
        public GetMyBillDetailByIdResponse getMyBillDetailById(Long id) {
                String userName = SecurityUtils.getCurrentUsername();
                User currentUser = userRepository.findByUsername(userName)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                Long currentUserId = currentUser.getId();
                Bill bill = billRepository.findByIdAndUserId(id, currentUserId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Bill not found or you don't have permission to view it"));

                return billMapper.toGetMyBillDetailByIdResponse(bill);
        }

}
