package com.ptithcm.apt.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ptithcm.apt.dto.request.BillRequest;
import com.ptithcm.apt.dto.request.CreateMonthlyMetricRequest;
import com.ptithcm.apt.dto.request.CreateRentInvoiceRequest;
import com.ptithcm.apt.dto.request.UpdateBillStatusRequest;
import com.ptithcm.apt.dto.response.CreateBillComboResponse;
import com.ptithcm.apt.dto.response.CreateBillResponse;
import com.ptithcm.apt.dto.response.CreateMonthlyMetricResponse;
import com.ptithcm.apt.dto.response.CreateRentInvoiceResponse;
import com.ptithcm.apt.dto.response.GetBillDetailByAdminResponse;
import com.ptithcm.apt.dto.response.GetBillsByAdminResponse;
import com.ptithcm.apt.dto.response.GetMyBillDetailByIdResponse;
import com.ptithcm.apt.dto.response.GetMyBillsResponse;
import com.ptithcm.apt.dto.response.UpdateBillStatusResponse;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.entity.MonthlyMetric;
import com.ptithcm.apt.entity.ServiceConfig;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.mappers.BillMapper;
import com.ptithcm.apt.mappers.MonthlyMetricMapper;
import com.ptithcm.apt.repository.ApartmentRepository;
import com.ptithcm.apt.repository.BillRepository;
import com.ptithcm.apt.repository.MonthlyMetricRepository;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.repository.ServiceConfigRepository;
import com.ptithcm.apt.repository.UserRepository;
import com.ptithcm.apt.repository.specifications.BillSpecifications;
import com.ptithcm.apt.service.BillService;
import com.ptithcm.apt.service.MonthlyMetricService;
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
        private final ServiceConfigRepository serviceConfigRepository;
        private final MonthlyMetricService monthlyMetricService;
        private final MonthlyMetricRepository monthlyMetricRepository;
        private final ResidentApartmentRepository residentApartmentRepository;

        @Override
        @Transactional
        public CreateBillComboResponse createBill(BillRequest req) {

                List<ServiceConfig> configs = serviceConfigRepository.findAllCurrentConfigs();
                Map<String, BigDecimal> priceMap = configs.stream()
                                .collect(Collectors.toMap(ServiceConfig::getServiceCode, ServiceConfig::getUnitPrice));

                Apartment apt = apartmentRepository.findById(req.apartmentId())
                                .orElseThrow(() -> new RuntimeException("Apartment not found"));
                if ("AVAILABLE".equals(apt.getStatus())) {
                        throw new RuntimeException(
                                        "Cannot create a bill for an AVAILABLE apartment.");
                }

                MonthlyMetric lastMetric = monthlyMetricRepository
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
                User currentUser = userRepository.findByUsername(userName)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                BigDecimal oldElec = (lastMetric != null) ? lastMetric.getElectricityNew() : BigDecimal.ZERO;
                BigDecimal oldWater = (lastMetric != null) ? lastMetric.getWaterNew() : BigDecimal.ZERO;

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
                                .createdBy(currentUser)
                                .status(BillStatus.UNPAID)
                                .build();
                billRepository.save(bill);

                CreateBillResponse billRes = billMapper.toCreateBillResponse(bill);

                CreateRentInvoiceResponse rentRes = null;
                CreateMonthlyMetricResponse metricRes = null;

                boolean shouldCreateRentInvoice = "RENTED".equals(apt.getStatus())
                                || residentApartmentRepository.existsByApartmentIdAndRoleAndIsActiveTrue(apt.getId(),
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

                return CreateBillComboResponse.builder()
                                .bill(billRes)
                                .rentInvoice(rentRes)
                                .monthlyMetric(metricRes)
                                .build();
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

        @Override
        public GetBillDetailByAdminResponse getBillDetailByAdmin(Long id) {
                Bill bill = billRepository.findById(id).orElseThrow(() -> new NotFoundException("Bill not found"));
                return billMapper.toGetBillDetailByAdminResponse(bill);
        }

}
