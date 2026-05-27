package com.ptithcm.apt.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.ptithcm.apt.repository.specifications.RentInvoiceSpecifications;
import com.ptithcm.apt.service.EmailService;
import com.ptithcm.apt.service.RentInvoiceService;
import com.ptithcm.apt.service.ResidentApartmentService;
import com.ptithcm.apt.service.ResidentService;
import com.ptithcm.apt.service.UserService;
import com.ptithcm.apt.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RentInvoiceServiceImpl implements RentInvoiceService {
        private final RentInvoiceRepository rentInvoiceRepository;
        private final ResidentApartmentService residentApartmentService;
        private final RentInvoiceMapper rentInvoiceMapper;
        private final UserService userService;
        private final ResidentService residentService;
        private final EmailService emailService;

        @Override
        @Transactional
        public RentInvoiceResponse createMonthlyRentInvoice(CreateRentInvoiceRequest req) {
                ResidentApartment contract = residentApartmentService.findActiveTenant(req.apartmentId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Căn hộ được đánh dấu là ĐANG THUÊ nhưng không tìm thấy hợp đồng thuê nào còn hiệu lực!"));

                LocalDate now = LocalDate.now();
                LocalDate billingDate = LocalDate.of(req.year(), req.month(), 1);

                if (contract.getContractEnd() != null && contract.getContractEnd().isBefore(now)) {
                        throw new RuntimeException("Không thể tạo hóa đơn: Hợp đồng của cư dân ["
                                        + contract.getResident().getFullName() + "] đã hết hạn vào ngày "
                                        + contract.getContractEnd());
                }

                if (contract.getContractEnd() != null && contract.getContractEnd().isBefore(billingDate)) {
                        throw new RuntimeException(
                                        "Hợp đồng sẽ hết hạn trước khi bắt đầu kỳ thanh toán này ("
                                                        + req.month()
                                                        + "/" + req.year() + ").");
                }

                Resident owner = residentApartmentService.findActiveOwner(req.apartmentId()).orElse(null);
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
                        RentStatus status, String roomNumber,Pageable pageable) {
                Specification<RentInvoice> spec = RentInvoiceSpecifications.hasFilters(month, year, apartmentId,
                                status, roomNumber);
                Page<RentInvoice> rentInvoices = rentInvoiceRepository.findAll(spec, pageable);

                return rentInvoices.map(rentInvoiceMapper::toGetRentInvoiceListResponse);
        }

        @Override
        public AdminRentInvoiceDetailResponse getRentInvoiceDetailByAdmin(Long id) {
                RentInvoice rentInvoice = rentInvoiceRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn thuê nhà"));
                return rentInvoiceMapper.toGetRentInvoiceDetailResponse(rentInvoice);
        }

        @Override
        public UpdateRentInvoiceStatusResponse updateRentInvoiceStatus(Long id, UpdateRentInvoiceStatusRequest req) {
                RentInvoice rentInvoice = rentInvoiceRepository.findById(id)
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn thuê nhà"));

                RentStatus currentStatus = rentInvoice.getStatus();
                RentStatus newStatus = req.status();

                if (newStatus != RentStatus.PAID) {
                        throw new RuntimeException("Hệ thống chỉ hỗ trợ cập nhật trạng thái sang ĐÃ THANH TOÁN");
                }

                if (currentStatus == RentStatus.PAID) {
                        throw new RuntimeException("Hóa đơn đã được thanh toán");
                }

                if (currentStatus != RentStatus.UNPAID && currentStatus != RentStatus.LATE) {
                        throw new RuntimeException("Không thể thanh toán hóa đơn với trạng thái hiện tại: " + currentStatus);
                }

                rentInvoice.setStatus(RentStatus.PAID);
                rentInvoice.setPaidAt(LocalDateTime.now());

                String username = SecurityUtils.getCurrentUsername();
                User currentUser = userService.findByUsername(username);
                                

                rentInvoice.setConfirmedBy(currentUser);

                rentInvoiceRepository.save(rentInvoice);
                return rentInvoiceMapper.toUpdateBillStatusResponse(rentInvoice);
        }

        @Override
        public Page<UserRentInvoiceListResponse> getMyRentInvoices(Integer month, Integer year, Long apartmentId,
                        RentStatus status, Pageable pageable) {
                String userName = SecurityUtils.getCurrentUsername();
                User currentUser = userService.findByUsername(userName);

                Long currentUserId = currentUser.getId();

                // Lấy resident của user hiện tại để xác định viewerRole
                Resident currentResident = residentService.findByUserId(currentUserId);
                                

                Page<RentInvoice> rentInvoices = rentInvoiceRepository.findMyRentInvoices(
                                currentUserId, apartmentId, month, year, status, pageable);

                return rentInvoices.map(ri -> {
                        boolean isTenant = currentResident.getId().equals(ri.getTenant().getId());
                        String viewerRole = isTenant ? "TENANT" : "OWNER";

                        String tenantName = null;
                        if (!isTenant && ri.getTenant().getId() != null) {
                                tenantName = residentService.findById(ri.getTenant().getId())
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
                User currentUser = userService.findByUsername(userName);
                Long currentUserId = currentUser.getId();
                RentInvoice rentInvoice = rentInvoiceRepository.findByIdAndUserId(id, currentUserId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Không tìm thấy hóa đơn thuê nhà hoặc bạn không có quyền xem hóa đơn này"));
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

        @Override
        public Optional<RentInvoice> findRentInvoiceEntityById(Long id) {
                return rentInvoiceRepository.findById(id);
        }

        @Override
        public Optional<RentInvoice> findRentInvoiceByIdAndUserId(Long id, Long userId) {
                return rentInvoiceRepository.findByIdAndUserId(id, userId);
        }

        @Override
        public List<RentInvoice> findAllByStatusAndDueDateBefore(RentStatus status, LocalDateTime dateTime) {
                return rentInvoiceRepository.findAllByStatusAndDueDateBefore(status, dateTime);
        }

        @Override
        public Page<RentInvoice> findMyRentInvoices(Long userId, Long apartmentId, Integer month, Integer year,
                        RentStatus status, Pageable pageable) {
                return rentInvoiceRepository.findMyRentInvoices(userId, apartmentId, month, year, status, pageable);
        }
}
