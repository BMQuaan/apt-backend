package com.ptithcm.apt.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.ptithcm.apt.entity.MonthlyMetric;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.ServiceConfig;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.mapper.BillMapper;
import com.ptithcm.apt.repository.BillRepository;
import com.ptithcm.apt.repository.specifications.BillSpecifications;
import com.ptithcm.apt.service.ApartmentService;
import com.ptithcm.apt.service.BillService;
import com.ptithcm.apt.service.EmailService;
import com.ptithcm.apt.service.MonthlyMetricService;
import com.ptithcm.apt.service.RentInvoiceService;
import com.ptithcm.apt.service.ResidentApartmentService;
import com.ptithcm.apt.service.ResidentService;
import com.ptithcm.apt.service.ServiceConfigService;
import com.ptithcm.apt.service.UserService;
import com.ptithcm.apt.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

        private final BillRepository billRepository;
        private final ApartmentService apartmentService;
        private final UserService userService;
        private final BillMapper billMapper;
        private final RentInvoiceService rentInvoiceService;
        private final ServiceConfigService serviceConfigService;
        private final MonthlyMetricService monthlyMetricService;
        private final ResidentApartmentService residentApartmentService;
        private final EmailService emailService;
        private final ResidentService residentService;

        @Override
        @Transactional
        public BillSummaryResponse createBill(CreateBillRequest req) {
                List<ServiceConfig> configs = serviceConfigService.findAllCurrentConfigs();
                Map<String, BigDecimal> priceMap = configs.stream()
                                .collect(Collectors.toMap(ServiceConfig::getServiceCode, ServiceConfig::getUnitPrice));

                Apartment apt = apartmentService.findById(req.apartmentId()).orElseThrow(() -> new NotFoundException(
                                "Apartment not found"));

                if ("AVAILABLE".equals(apt.getStatus())) {
                        throw new RuntimeException(
                                        "Cannot create a bill for an AVAILABLE apartment.");
                }

                MonthlyMetric lastMetric = monthlyMetricService
                                .findFirstByApartmentIdOrderByBillingYearDescBillingMonthDesc(apt.getId())
                                .orElse(null);

                if (lastMetric != null) {
                        if (req.year() < lastMetric.getBillingYear() ||
                                        (req.year().equals(lastMetric.getBillingYear())
                                                        && req.month() <= lastMetric.getBillingMonth())) {
                                throw new RuntimeException(
                                                "Cannot create bill for a period that already has metrics or is in the past.");
                        }
                }

                String userName = SecurityUtils.getCurrentUsername();
                User currentUser = userService.findByUsername(userName);

                BigDecimal oldElec = (lastMetric != null) ? lastMetric.getElectricityNew() : BigDecimal.ZERO;
                BigDecimal oldWater = (lastMetric != null) ? lastMetric.getWaterNew() : BigDecimal.ZERO;

                if (req.electricityService().compareTo(oldElec) < 0) {
                        throw new RuntimeException(
                                        "New electricity index (" + req.electricityService()
                                                        + ") cannot be less than old index (" + oldElec + ")");
                }
                if (req.waterService().compareTo(oldWater) < 0) {
                        throw new RuntimeException(
                                        "New water index (" + req.waterService()
                                                        + ") cannot be less than old index (" + oldWater + ")");
                }

                BigDecimal waterFee = priceMap.get("WATER")
                                .multiply(BigDecimal.valueOf(req.waterService().longValue()).subtract(oldWater));
                BigDecimal electricityFee = priceMap.get("ELECTRICITY")
                                .multiply(BigDecimal.valueOf(req.electricityService().longValue()).subtract(oldElec));
                BigDecimal managementFee = priceMap.get("MANAGEMENT").multiply(apt.getArea());
                BigDecimal sanitationFee = priceMap.get("SANITATION");

                BigDecimal totalAmount = waterFee
                                .add(managementFee)
                                .add(sanitationFee)
                                .add(electricityFee);

                Bill bill = Bill.builder()
                                .apartment(apt)
                                .billingMonth(req.month())
                                .billingYear(req.year())
                                .waterFee(waterFee)
                                .managementFee(managementFee)
                                .sanitationFee(sanitationFee)
                                .electricityFee(electricityFee)
                                .totalAmount(totalAmount)
                                // .createdAt(testDate)
                                .createdBy(currentUser)
                                .status(BillStatus.UNPAID)
                                .build();
                billRepository.save(bill);

                BillResponse billRes = billMapper.toCreateBillResponse(bill);

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

        @Override
        public Page<AdminBillListResponse> getBillsByAdmin(Integer month, Integer year, Long apartmentId,
                        BillStatus status, String roomNumber, Pageable pageable) {
                Specification<Bill> spec = BillSpecifications.hasFilters(month, year, apartmentId, status, roomNumber);
                Page<Bill> bills = billRepository.findAll(spec, pageable);
                return bills.map(billMapper::toGetBillsByAdminResponse);
        }

        @Override
        public Page<UserBillListResponse> getMyBills(Integer month, Integer year, Long apartmentId, BillStatus status,
                        Pageable pageable) {
                String userName = SecurityUtils.getCurrentUsername();
                User currentUser = userService.findByUsername(userName);

                Resident currentResident = residentService.findByUserId(currentUser.getId());

                Long currentUserId = currentUser.getId();
                Page<Bill> bills = billRepository.findMyBills(currentUserId, apartmentId, month, year, status,
                                pageable);

                // Gom tất cả apartmentId để tránh N+1
                Set<Long> apartmentIds = bills.stream()
                                .map(b -> b.getApartment().getId())
                                .collect(Collectors.toSet());

                Map<Long, Resident> tenantByApartment = apartmentIds.stream()
                                .flatMap(aptId -> residentApartmentService
                                                .findActiveTenant(aptId)
                                                .stream())
                                .collect(Collectors.toMap(
                                                ra -> ra.getApartment().getId(),
                                                ResidentApartment::getResident));

                return bills.map(b -> {
                        Long aptId = b.getApartment().getId();
                        Resident tenant = tenantByApartment.get(aptId);

                        // isHead = true → đang là chủ hộ (tự ở hoặc đang thuê đứng tên)
                        // isHead = false → OWNER không ở đây, đang cho thuê
                        boolean isHead = residentApartmentService
                                        .existsByApartmentIdAndResidentIdAndIsHeadTrueAndIsActiveTrue(
                                                        aptId, currentResident.getId());

                        return UserBillListResponse.builder()
                                        .id(b.getId())
                                        .apartmentName(b.getApartment().getRoomNumber())
                                        .billingMonth(b.getBillingMonth())
                                        .billingYear(b.getBillingYear())
                                        .electricityFee(b.getElectricityFee())
                                        .waterFee(b.getWaterFee())
                                        .managementFee(b.getManagementFee())
                                        .sanitationFee(b.getSanitationFee())
                                        .totalAmount(b.getTotalAmount())
                                        .status(b.getStatus())
                                        .viewerRole(isHead ? "HEAD" : "OWNER")
                                        .tenantName(!isHead && tenant != null
                                                        ? tenant.getFullName()
                                                        : null)
                                        .dueDate(b.getDueDate())
                                        .build();
                });
        }

        @Override
        public UserBillDetailResponse getMyBillDetailById(Long id) {
                String userName = SecurityUtils.getCurrentUsername();
                User currentUser = userService.findByUsername(userName);

                Long currentUserId = currentUser.getId();
                Bill bill = billRepository.findByIdAndUserId(id, currentUserId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Bill not found or you don't have permission to view it"));

                return billMapper.toGetMyBillDetailByIdResponse(bill);
        }

        @Override
        public AdminBillDetailResponse getBillDetailByAdmin(Long id) {
                Bill bill = billRepository.findById(id).orElseThrow(() -> new NotFoundException("Bill not found"));
                return billMapper.toGetBillDetailByAdminResponse(bill);
        }

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
