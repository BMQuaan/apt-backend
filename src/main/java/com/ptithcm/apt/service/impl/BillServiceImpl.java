package com.ptithcm.apt.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ptithcm.apt.dto.BillFeeResult;
import com.ptithcm.apt.dto.BillValidationResult;
import com.ptithcm.apt.dto.request.CreateBillRequest;
import com.ptithcm.apt.dto.request.CreateMonthlyMetricRequest;
import com.ptithcm.apt.dto.request.CreateRentInvoiceRequest;
import com.ptithcm.apt.dto.request.UpdateBillStatusRequest;
import com.ptithcm.apt.dto.response.BillSummaryResponse;
import com.ptithcm.apt.dto.response.BillResponse;
import com.ptithcm.apt.dto.response.MonthlyMetricResponse;
import com.ptithcm.apt.dto.response.RentInvoiceResponse;
import com.ptithcm.apt.dto.response.AdminBillDetailResponse;
import com.ptithcm.apt.dto.response.AdminBillListResponse;
import com.ptithcm.apt.dto.response.UserBillDetailResponse;
import com.ptithcm.apt.dto.response.UserBillListResponse;
import com.ptithcm.apt.dto.response.UpdateBillStatusResponse;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.mapper.BillMapper;
import com.ptithcm.apt.repository.BillRepository;
import com.ptithcm.apt.service.BillService;
import com.ptithcm.apt.service.EmailService;
import com.ptithcm.apt.service.MonthlyMetricService;
import com.ptithcm.apt.service.RentInvoiceService;
import com.ptithcm.apt.service.ResidentApartmentService;
import com.ptithcm.apt.service.UserService;
import com.ptithcm.apt.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

        // --- Core dependencies ---
        private final BillRepository billRepository;
        private final BillMapper billMapper;
        private final UserService userService;
        private final RentInvoiceService rentInvoiceService;
        private final MonthlyMetricService monthlyMetricService;
        private final ResidentApartmentService residentApartmentService;
        private final EmailService emailService;

        // --- Delegated services ---
        private final BillValidationService billValidationService;
        private final BillCalculationService billCalculationService;
        private final AdminBillQueryService adminBillQueryService;
        private final UserBillQueryService userBillQueryService;

        @Override
        @Transactional
        public BillSummaryResponse createBill(CreateBillRequest req) {
                // 1. Validate nghiệp vụ
                BillValidationResult validated = billValidationService.validateCreateBill(req);
                Apartment apt = validated.apartment();
                BigDecimal oldElec = validated.oldElectricity();
                BigDecimal oldWater = validated.oldWater();

                // 2. Tính phí dịch vụ
                BillFeeResult fees = billCalculationService.calculateFees(
                                apt, req.electricityService(), oldElec, req.waterService(), oldWater);

                // 3. Lấy user hiện tại
                String userName = SecurityUtils.getCurrentUsername();
                User currentUser = userService.findByUsername(userName);

                // 4. Tạo và lưu Bill
                Bill bill = Bill.builder()
                                .apartment(apt)
                                .billingMonth(req.month())
                                .billingYear(req.year())
                                .waterFee(fees.waterFee())
                                .managementFee(fees.managementFee())
                                .sanitationFee(fees.sanitationFee())
                                .electricityFee(fees.electricityFee())
                                .totalAmount(fees.totalAmount())
                                .createdBy(currentUser)
                                .status(BillStatus.UNPAID)
                                .build();
                billRepository.save(bill);

                BillResponse billRes = billMapper.toCreateBillResponse(bill);

                // 5. Tạo RentInvoice & MonthlyMetric (side effects)
                RentInvoiceResponse rentRes = null;
                MonthlyMetricResponse metricRes = null;

                boolean shouldCreateRentInvoice = "RENTED".equals(apt.getStatus())
                                || residentApartmentService.existsByApartmentIdAndRoleAndIsActiveTrue(apt.getId(),
                                                "TENANT");
                if (shouldCreateRentInvoice) {
                        CreateRentInvoiceRequest rentReq = new CreateRentInvoiceRequest(
                                        apt.getId(),
                                        bill.getBillingMonth(),
                                        bill.getBillingYear(),
                                        currentUser);
                        rentRes = rentInvoiceService.createMonthlyRentInvoice(rentReq);
                }

                CreateMonthlyMetricRequest metricRequest = new CreateMonthlyMetricRequest(
                                apt,
                                bill.getBillingMonth(),
                                bill.getBillingYear(),
                                req.electricityService(),
                                req.waterService(),
                                oldElec,
                                oldWater);
                metricRes = monthlyMetricService.createMonthlyMetric(metricRequest);

                if (bill.getDueDate() == null) {
                        bill.setDueDate(LocalDateTime.now().plusDays(15));
                }

                // 6. Gửi email thông báo
                try {
                        ResidentApartment headResident = residentApartmentService
                                        .findByApartmentIdAndIsHeadTrueAndIsActiveTrue(apt.getId())
                                        .orElse(null);

                        if (headResident != null && headResident.getResident().getEmail() != null) {

                                Map<String, String> templateModel = Map.of(
                                                "fullName", headResident.getResident().getFullName(),
                                                "roomNumber", apt.getRoomNumber(),
                                                "month", String.valueOf(bill.getBillingMonth()),
                                                "year", String.valueOf(bill.getBillingYear()),
                                                "electricityFee", String.format("%,.0f", bill.getElectricityFee()),
                                                "waterFee", String.format("%,.0f", bill.getWaterFee()),
                                                "managementFee", String.format("%,.0f", bill.getManagementFee()),
                                                "sanitationFee", String.format("%,.0f", bill.getSanitationFee()),
                                                "totalAmount", String.format("%,.0f", bill.getTotalAmount()),
                                                "dueDate", bill.getDueDate().format(java.time.format.DateTimeFormatter
                                                                .ofPattern("dd/MM/yyyy")));

                                emailService.sendHtmlEmail(
                                                headResident.getResident().getEmail(),
                                                "[AptApp] Thông báo phí dịch vụ tháng " + bill.getBillingMonth() + "/"
                                                                + bill.getBillingYear() + " - Phòng "
                                                                + apt.getRoomNumber(),
                                                "new_service_bill_template_vi.html",
                                                templateModel);
                        }
                } catch (Exception e) {
                }

                return BillSummaryResponse.builder()
                                .bill(billRes)
                                .rentInvoice(rentRes)
                                .monthlyMetric(metricRes)
                                .build();
        }

        @Override
        public UpdateBillStatusResponse updateBillStatus(Long billId, UpdateBillStatusRequest req) {
                Bill bill = billRepository.findById(billId)
                                .orElseThrow(() -> new RuntimeException("Bill not found"));

                BillStatus currentStatus = bill.getStatus();
                BillStatus newStatus = req.status();

                if (newStatus != BillStatus.PAID) {
                        throw new RuntimeException("Only PAID status transition is supported");
                }

                if (currentStatus == BillStatus.PAID) {
                        throw new RuntimeException("Bill is already PAID");
                }

                if (currentStatus != BillStatus.UNPAID && currentStatus != BillStatus.LATE) {
                        throw new RuntimeException("Cannot pay bill with current status: " + currentStatus);
                }

                bill.setStatus(BillStatus.PAID);
                bill.setPaidAt(LocalDateTime.now());

                String username = SecurityUtils.getCurrentUsername();
                User currentUser = userService.findByUsername(username);

                bill.setConfirmedBy(currentUser);

                billRepository.save(bill);
                return billMapper.toUpdateBillStatusResponse(bill);
        }

        // --- Delegate to AdminBillQueryService ---

        @Override
        public Page<AdminBillListResponse> getBillsByAdmin(Integer month, Integer year, Long apartmentId,
                        BillStatus status, String roomNumber, Pageable pageable) {
                return adminBillQueryService.getBillsByAdmin(month, year, apartmentId, status, roomNumber, pageable);
        }

        @Override
        public AdminBillDetailResponse getBillDetailByAdmin(Long id) {
                return adminBillQueryService.getBillDetailByAdmin(id);
        }

        // --- Delegate to UserBillQueryService ---

        @Override
        public Page<UserBillListResponse> getMyBills(Integer month, Integer year, Long apartmentId, BillStatus status,
                        Pageable pageable) {
                return userBillQueryService.getMyBills(month, year, apartmentId, status, pageable);
        }

        @Override
        public UserBillDetailResponse getMyBillDetailById(Long id) {
                return userBillQueryService.getMyBillDetailById(id);
        }

        // --- Repository delegates (giữ lại cho scheduler và các service khác) ---

        @Override
        public Optional<Bill> findBillEntityById(Long id) {
                return billRepository.findById(id);
        }

        @Override
        public Optional<Bill> findBillByIdAndUserId(Long billId, Long userId) {
                return billRepository.findByIdAndUserId(billId, userId);
        }

        @Override
        public List<Bill> findAllByStatusAndDueDateBefore(BillStatus status, LocalDateTime dateTime) {
                return billRepository.findAllByStatusAndDueDateBefore(status, dateTime);
        }

        @Override
        public boolean isApartmentHasStatus(Long apartmentId, BillStatus status) {
                return billRepository.existsByApartmentIdAndStatus(apartmentId, status);
        }

}
