package com.ptithcm.apt.service.impl;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ptithcm.apt.dto.response.UserBillDetailResponse;
import com.ptithcm.apt.dto.response.UserBillListResponse;
import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.mapper.BillMapper;
import com.ptithcm.apt.repository.BillRepository;
import com.ptithcm.apt.service.ResidentApartmentService;
import com.ptithcm.apt.service.ResidentService;
import com.ptithcm.apt.service.UserService;
import com.ptithcm.apt.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserBillQueryService {

        private final BillRepository billRepository;
        private final BillMapper billMapper;
        private final UserService userService;
        private final ResidentService residentService;
        private final ResidentApartmentService residentApartmentService;

        /**
         * Lấy danh sách bill cho User hiện tại với filter.
         */
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

        /**
         * Lấy chi tiết bill cho User hiện tại.
         */
        public UserBillDetailResponse getMyBillDetailById(Long id) {
                String userName = SecurityUtils.getCurrentUsername();
                User currentUser = userService.findByUsername(userName);

                Long currentUserId = currentUser.getId();
                Bill bill = billRepository.findByIdAndUserId(id, currentUserId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Không tìm thấy hóa đơn hoặc bạn không có quyền xem hóa đơn này"));

                return billMapper.toGetMyBillDetailByIdResponse(bill);
        }
}
