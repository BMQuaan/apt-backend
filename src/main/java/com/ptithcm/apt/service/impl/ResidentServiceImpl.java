package com.ptithcm.apt.service.impl;

import com.ptithcm.apt.dto.request.ContractRequest;
import com.ptithcm.apt.dto.request.MemberRequest;
import com.ptithcm.apt.dto.request.ResidentRequest;
import com.ptithcm.apt.dto.request.UpdateResidentRequest;
import com.ptithcm.apt.dto.response.MyApartmentResponse;
import com.ptithcm.apt.dto.response.ResidentDetailResponse;
import com.ptithcm.apt.dto.response.ResidentListResponse;
import com.ptithcm.apt.dto.response.ResidentResponse;
import com.ptithcm.apt.entity.*;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.repository.*;
import com.ptithcm.apt.service.ResidentService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResidentServiceImpl implements ResidentService {

        private final ResidentRepository residentRepository;
        private final ResidentApartmentRepository residentApartmentRepository;
        private final UserRepository userRepository;
        private final ApartmentRepository apartmentRepository;
        private final BillRepository billRepository;

        @Override
        public Optional<String> findNameByUserId(Long userId) {
                return residentRepository.findByUser_Id(userId)
                                .map(Resident::getFullName);
        }

        @Override
        @Transactional
        public ResidentResponse addMemberToApartment(String roomNumber, MemberRequest request) {
                Apartment apartment = apartmentRepository.findByRoomNumber(roomNumber)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với ID: " + roomNumber));

                ResidentApartment headContract = residentApartmentRepository
                                .findByApartmentIdAndIsHeadTrueAndIsActiveTrue(apartment.getId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Phòng này chưa có người thuê/chủ hộ, không thể thêm thành viên ở ghép!"));

                Resident resident;
                var existingResidentByEmail = residentRepository.findByEmail(request.getEmail());
                var existingResidentByCccd = residentRepository.findByCitizenIdentity(request.getCitizenIdentity());

                if (existingResidentByEmail.isPresent()) {
                        resident = existingResidentByEmail.get();
                        if (!resident.getCitizenIdentity().equals(request.getCitizenIdentity())) {
                                throw new RuntimeException("Email này đã được đăng ký cho một CCCD khác!");
                        }
                } else if (existingResidentByCccd.isPresent()) {
                        throw new RuntimeException("Căn cước công dân này đã tồn tại với Email khác!");
                } else {
                        // Nếu là người mới hoàn toàn
                        Resident newResident = Resident.builder()
                                        .fullName(request.getFullName())
                                        .dob(request.getDob())
                                        .phone(request.getPhone())
                                        .citizenIdentity(request.getCitizenIdentity())
                                        .email(request.getEmail())
                                        .build();
                        resident = residentRepository.save(newResident);
                }

                ResidentApartment memberRecord = ResidentApartment.builder()
                                .resident(resident)
                                .apartment(apartment)
                                .role("MEMBER")
                                .isHead(false)
                                .isActive(true)
                                .rentalPrice(BigDecimal.ZERO)
                                .depositAmount(BigDecimal.ZERO)
                                .contractStart(headContract.getContractStart())
                                .contractEnd(headContract.getContractEnd())
                                .build();
                residentApartmentRepository.save(memberRecord);

                return ResidentResponse.builder()
                                .id(resident.getId())
                                .fullName(resident.getFullName())
                                .citizenIdentity(resident.getCitizenIdentity())
                                .dob(resident.getDob())
                                .phone(resident.getPhone())
                                .email(resident.getEmail())
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public ResidentDetailResponse getResidentDetailById(Long residentId) {
                Resident resident = residentRepository.findById(residentId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân!"));

                List<ResidentApartment> activeRas = residentApartmentRepository
                                .findAllByResidentIdAndIsActiveTrue(residentId);

                List<ResidentDetailResponse.ResidencyInfo> residencies = activeRas.stream()
                                .map(ra -> ResidentDetailResponse.ResidencyInfo.builder()
                                                .apartmentId(ra.getApartment().getId())
                                                .roomNumber(ra.getApartment().getRoomNumber())
                                                .role(ra.getRole())
                                                .isHead(ra.getIsHead())
                                                .build())
                                .collect(Collectors.toList());
                return ResidentDetailResponse.builder()
                                .id(resident.getId())
                                .fullName(resident.getFullName())
                                .citizenIdentity(resident.getCitizenIdentity())
                                .dob(resident.getDob() != null ? resident.getDob().toString() : null)
                                .phone(resident.getPhone())
                                .email(resident.getEmail())
                                .residencies(residencies) // Trả về toàn bộ danh sách
                                .build();
        }

        @Override
        @Transactional
        public ResidentDetailResponse updateResident(Long residentId, UpdateResidentRequest request) {
                Resident resident = residentRepository.findById(residentId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân!"));

                if (request.getFullName() != null && !request.getFullName().isBlank()) {
                        resident.setFullName(request.getFullName());
                }

                if (request.getPhone() != null && !request.getPhone().isBlank()) {
                        resident.setPhone(request.getPhone());
                }

                if (request.getDob() != null) {
                        resident.setDob(request.getDob());
                }

                if (request.getCitizenIdentity() != null && !request.getCitizenIdentity().isBlank()) {
                        String newCccd = request.getCitizenIdentity();
                        if (!newCccd.equals(resident.getCitizenIdentity())) {
                                if (residentRepository.existsByCitizenIdentity(newCccd)) {
                                        throw new RuntimeException(
                                                        "Căn cước công dân mới này đã tồn tại trong hệ thống!");
                                }
                                resident.setCitizenIdentity(newCccd);
                        }
                }

                if (request.getEmail() != null && !request.getEmail().isBlank()) {
                        String newEmail = request.getEmail();
                        if (!newEmail.equals(resident.getEmail())) {
                                if (residentRepository.existsByEmail(newEmail)) {
                                        throw new RuntimeException("Email này đã có người sử dụng!");
                                }
                                resident.setEmail(newEmail);

                                User user = resident.getUser();
                                if (user != null) {
                                        user.setUsername(newEmail);
                                        userRepository.save(user);
                                }
                        }
                }

                Resident updatedResident = residentRepository.save(resident);

                return getResidentDetailById(updatedResident.getId());
        }

        @Override
        @Transactional(readOnly = true)
        public Page<ResidentListResponse> getActiveResidents(String keyword, Pageable pageable) {
                Page<ResidentApartment> pageData = residentApartmentRepository.searchAllActiveResidents(keyword,
                                pageable);

                return pageData.map(ra -> ResidentListResponse.builder()
                                .residentId(ra.getResident().getId())
                                .fullName(ra.getResident().getFullName())
                                .citizenIdentity(ra.getResident().getCitizenIdentity())
                                .phone(ra.getResident().getPhone())
                                .roomNumber(ra.getApartment().getRoomNumber())
                                .role(ra.getRole())
                                .isHead(ra.getIsHead())
                                .contractStart(ra.getContractStart())
                                .build());
        }

        @Override
        @Transactional
        public void moveOutResident(Long residentId, Long apartmentId) {
                boolean hasUnpaidBills = billRepository.existsByApartmentIdAndStatus(apartmentId, BillStatus.UNPAID);
                if (hasUnpaidBills) {
                        throw new RuntimeException("Không thể chuyển đi! Căn hộ này vẫn còn hóa đơn chưa thanh toán.");
                }

                ResidentApartment ra = residentApartmentRepository
                                .findByResidentIdAndApartmentIdAndIsActiveTrue(residentId, apartmentId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Không tìm thấy thông tin cư trú hợp lệ để chuyển đi!"));

                if (ra.getIsHead()) {
                        List<ResidentApartment> allMembers = residentApartmentRepository
                                        .findByApartmentIdAndIsActiveTrue(apartmentId);
                        for (ResidentApartment member : allMembers) {
                                member.setIsActive(false);
                                member.setContractEnd(LocalDate.now());

                                // Kiểm tra và khóa tài khoản cho từng người
                                disableUserAccountIfNoActiveRoom(member.getResident());
                        }
                        residentApartmentRepository.saveAll(allMembers);

                        // Trả phòng về trạng thái trống (AVAILABLE)
                        Apartment apartment = ra.getApartment();
                        apartment.setStatus("AVAILABLE");
                        apartmentRepository.save(apartment);

                } else {

                        ra.setIsActive(false);
                        ra.setContractEnd(LocalDate.now());
                        residentApartmentRepository.save(ra);

                        // Kiểm tra và khóa tài khoản của riêng người này
                        disableUserAccountIfNoActiveRoom(ra.getResident());
                }
        }

        private void disableUserAccountIfNoActiveRoom(Resident resident) {
                boolean stillHasOtherApartment = residentApartmentRepository
                                .existsByResidentIdAndIsActiveTrue(resident.getId());
                if (!stillHasOtherApartment) {
                        User user = resident.getUser();
                        if (user != null && user.getIsActive()) {
                                user.setIsActive(false);
                                userRepository.save(user);
                        }
                }
        }

        @Override
        @Transactional(readOnly = true)
        public List<ResidentListResponse> getResidentsByApartment(Long apartmentId) {

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication != null && authentication.isAuthenticated()) {
                        String currentUsername = authentication.getName();
                        boolean isAdmin = authentication.getAuthorities().stream()
                                        .anyMatch(role -> role.getAuthority().equals("ADMIN")
                                                        || role.getAuthority().equals("ROLE_ADMIN"));

                        if (!isAdmin) {
                                Resident currentResident = residentRepository.findByEmail(currentUsername)
                                                .orElseThrow(() -> new AccessDeniedException(
                                                                "Tài khoản của bạn không được liên kết với cư dân nào!"));

                                boolean isLivingHere = residentApartmentRepository
                                                .findByResidentIdAndApartmentIdAndIsActiveTrue(currentResident.getId(),
                                                                apartmentId)
                                                .isPresent();

                                if (!isLivingHere) {
                                        throw new AccessDeniedException(
                                                        "Lỗi bảo mật: Bạn chỉ được phép xem thông tin của phòng mình đang ở!");
                                }
                        }
                }
                if (!apartmentRepository.existsById(apartmentId)) {
                        throw new RuntimeException("Không tìm thấy căn hộ với ID:" + apartmentId);
                }

                List<ResidentApartment> list = residentApartmentRepository
                                .findByApartmentIdAndIsActiveTrue(apartmentId);

                return list.stream().map(ra -> ResidentListResponse.builder()
                                .residentId(ra.getResident().getId())
                                .fullName(ra.getResident().getFullName())
                                .citizenIdentity(ra.getResident().getCitizenIdentity())
                                .phone(ra.getResident().getPhone())
                                .roomNumber(ra.getApartment().getRoomNumber())
                                .role(ra.getRole())
                                .isHead(ra.getIsHead())
                                .contractStart(ra.getContractStart())
                                .build())
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<MyApartmentResponse> getMyApartments() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                        throw new RuntimeException("Chưa đăng nhập!");
                }
                String currentEmail = authentication.getName();

                Resident currentResident = residentRepository.findByEmail(currentEmail)
                                .orElseThrow(() -> new RuntimeException(
                                                "Tài khoản của bạn chưa được liên kết với hồ sơ cư dân nào!"));

                List<ResidentApartment> activeRooms = residentApartmentRepository
                                .findAllByResidentIdAndIsActiveTrue(currentResident.getId());

                return activeRooms.stream().map(ra -> MyApartmentResponse.builder()
                                .apartmentId(ra.getApartment().getId())
                                .roomNumber(ra.getApartment().getRoomNumber())
                                .role(ra.getRole())
                                .isHead(ra.getIsHead())
                                .rentalPrice(ra.getRentalPrice())
                                .contractStart(ra.getContractStart())
                                .contractEnd(ra.getContractEnd())
                                .build()).collect(Collectors.toList());
        }
}
