package com.ptithcm.apt.service.impl;

import java.security.Security;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

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
import com.ptithcm.apt.dto.response.UpdateRentInvoiceStatusResponse;
import com.ptithcm.apt.dto.response.UserRentInvoiceDetailResponse;
import com.ptithcm.apt.dto.response.UserRentInvoiceListResponse;
import com.ptithcm.apt.entity.RentInvoice;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.enums.RentStatus;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.mapper.RentInvoiceMapper;
import com.ptithcm.apt.repository.RentInvoiceRepository;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.repository.ResidentRepository;
import com.ptithcm.apt.repository.UserRepository;
import com.ptithcm.apt.repository.specifications.RentInvoiceSpecifications;
import com.ptithcm.apt.service.EmailService;
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
        private final ResidentRepository residentRepository;
        private final EmailService emailService;

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

                sendRentInvoiceEmail(invoice, contract);

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
        public UpdateRentInvoiceStatusResponse updateRentInvoiceStatus(Long id, UpdateRentInvoiceStatusRequest req) {
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
                                .orElseThrow(() -> new RuntimeException(
                                                "Authenticated user " + username + " not found"));

                rentInvoice.setConfirmedBy(currentUser);

                rentInvoiceRepository.save(rentInvoice);
                return rentInvoiceMapper.toUpdateBillStatusResponse(rentInvoice);
        }

        @Override
        public Page<UserRentInvoiceListResponse> getMyRentInvoices(Integer month, Integer year, Long apartmentId,
                        RentStatus status, Pageable pageable) {
                String userName = SecurityUtils.getCurrentUsername();
                User currentUser = userRepository.findByUsername(userName)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                Long currentUserId = currentUser.getId();

                // Lấy resident của user hiện tại để xác định viewerRole
                Resident currentResident = residentRepository.findByUser_Id(currentUserId)
                                .orElseThrow(() -> new RuntimeException("Resident not found"));

                Page<RentInvoice> rentInvoices = rentInvoiceRepository.findMyRentInvoices(
                                currentUserId, apartmentId, month, year, status, pageable);

                return rentInvoices.map(ri -> {
                        boolean isTenant = currentResident.getId().equals(ri.getTenant().getId());
                        String viewerRole = isTenant ? "TENANT" : "OWNER";

                        String tenantName = null;
                        if (!isTenant && ri.getTenant().getId() != null) {
                                tenantName = residentRepository.findById(ri.getTenant().getId())
                                                .map(Resident::getFullName)
                                                .orElse(null);
                        }
                        return UserRentInvoiceListResponse.builder()
                                        .id(ri.getId())
                                        .apartmentName(ri.getApartment().getRoomNumber())
                                        .billingMonth(ri.getBillingMonth())
                                        .billingYear(ri.getBillingYear())
                                        .rentAmount(ri.getRentAmount())
                                        .status(ri.getStatus())
                                        .dueDate(ri.getDueDate())
                                        .viewerRole(viewerRole)
                                        .tenantName(tenantName)
                                        .build();
                });
        }

        @Override
        public UserRentInvoiceDetailResponse getMyRentInvoiceDetailById(Long id) {
                String userName = SecurityUtils.getCurrentUsername();
                User currentUser = userRepository.findByUsername(userName)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                Long currentUserId = currentUser.getId();
                RentInvoice rentInvoice = rentInvoiceRepository.findByIdAndUserId(id, currentUserId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Rent invoice not found or you don't have permission to view it"));
                return rentInvoiceMapper.toMyRentInvoiceDetailResponse(rentInvoice);
        }

        private void sendRentInvoiceEmail(RentInvoice invoice, ResidentApartment contract) {
                Resident tenant = invoice.getTenant();
                if (tenant != null && tenant.getEmail() != null) {

                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime estimatedDueDate = now.plusDays(15);

                        String formattedDueDate = estimatedDueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                        Map<String, String> templateModel = Map.of(
                                        "fullName", tenant.getFullName(),
                                        "roomNumber", invoice.getApartment().getRoomNumber(),
                                        "month", String.valueOf(invoice.getBillingMonth()),
                                        "year", String.valueOf(invoice.getBillingYear()),
                                        "rentAmount", String.format("%,.0f", invoice.getRentAmount()),
                                        "dueDate", formattedDueDate);

                        emailService.sendHtmlEmail(
                                        tenant.getEmail(),
                                        "[AptApp] Thông báo tiền thuê nhà tháng " + invoice.getBillingMonth() + "/"
                                                        + invoice.getBillingYear() + " - Phòng "
                                                        + invoice.getApartment().getRoomNumber(),
                                        "new_rent_invoice_template_vi.html",
                                        templateModel);
                }
        }
}
