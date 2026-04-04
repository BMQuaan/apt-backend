package com.ptithcm.apt.service.impl;

import com.ptithcm.apt.dto.request.ContractRequest;
import com.ptithcm.apt.dto.request.MemberRequest;
import com.ptithcm.apt.dto.request.ResidentRequest;
import com.ptithcm.apt.dto.request.UpdateResidentRequest;
import com.ptithcm.apt.dto.response.ResidentListResponse;
import com.ptithcm.apt.dto.response.ResidentResponse;
import com.ptithcm.apt.entity.*;
import com.ptithcm.apt.repository.*;
import com.ptithcm.apt.service.ResidentService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
        private final RoleRepository roleRepository;
        private final ApartmentRepository apartmentRepository;
        private final PasswordEncoder passwordEncoder;

        // @Override
        // @Transactional
        // public ResidentResponse createContract(ContractRequest request) {

        // if (residentRepository.existsByEmail(request.getEmail())) {
        // throw new RuntimeException("Email này đã tồn tại trong hệ thống!");
        // }

        // if (residentRepository.existsByCitizenIdentity(request.getCitizenIdentity()))
        // {
        // throw new RuntimeException("Căn cước công dân này đã tồn tại trong hệ
        // thống!");
        // }
        // if (!"TENANT".equals(request.getRole()) &&
        // !"OWNER".equals(request.getRole())) {
        // throw new RuntimeException("Vai trò khi lập hợp đồng chỉ có thể là TENANT
        // hoặc OWNER");
        // }

        // if (request.getContractEnd() != null &&
        // request.getContractStart().isAfter(request.getContractEnd())) {
        // throw new RuntimeException("Ngày bắt đầu hợp đồng không thể nằm sau ngày kết
        // thúc!");
        // }

        // Apartment apartment = apartmentRepository.findById(request.getApartmentId())
        // .orElseThrow(() -> new RuntimeException(
        // "Không tìm thấy phòng với ID: " + request.getApartmentId()));

        // if (!"AVAILABLE".equals(apartment.getStatus())) {
        // throw new RuntimeException("Phòng này hiện không trống, không thể lập hợp
        // đồng mới!");
        // }

        // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        // String rawPassword = request.getDob().format(formatter);

        // // Tạo tài khoản User
        // Role userRole = roleRepository.findByRoleName("ROLE_USER")
        // .orElseThrow(() -> new RuntimeException("Chưa cấu hình Role USER trong DB"));

        // User newUser = User.builder()
        // .username(request.getEmail())
        // .password(passwordEncoder.encode(rawPassword))
        // .role(userRole)
        // .isActive(true)
        // .build();
        // User savedUser = userRepository.save(newUser);

        // Resident resident = Resident.builder()
        // .user(savedUser)
        // .fullName(request.getFullName())
        // .dob(request.getDob())
        // .phone(request.getPhone())
        // .citizenIdentity(request.getCitizenIdentity())
        // .email(request.getEmail())
        // .build();
        // Resident savedResident = residentRepository.save(resident);

        // ResidentApartment contract = ResidentApartment.builder()
        // .resident(savedResident)
        // .apartment(apartment)
        // .role(request.getRole())
        // .isHead(true)
        // .isActive(true)
        // .rentalPrice(request.getRentalPrice())
        // .depositAmount(request.getDepositAmount())
        // .contractStart(request.getContractStart())
        // .contractEnd(request.getContractEnd())
        // .build();
        // residentApartmentRepository.save(contract);

        // apartment.setStatus("TENANT".equals(request.getRole()) ? "RENTED" : "OWNED");
        // apartmentRepository.save(apartment);

        // return ResidentResponse.builder()
        // .id(savedResident.getId())
        // .fullName(savedResident.getFullName())
        // .citizenIdentity(savedResident.getCitizenIdentity())
        // .dob(savedResident.getDob())
        // .phone(savedResident.getPhone())
        // .email(savedResident.getEmail())
        // .build();
        // }

        @Override
        @Transactional
        public ResidentResponse addMemberToApartment(Long apartmentId, MemberRequest request) {

                if (residentRepository.existsByEmail(request.getEmail())) {
                        throw new RuntimeException("Email này đã tồn tại trong hệ thống!");
                }
                if (residentRepository.existsByCitizenIdentity(request.getCitizenIdentity())) {
                        throw new RuntimeException("Căn cước công dân này đã tồn tại trong hệ thống!");
                }
                Apartment apartment = apartmentRepository.findById(apartmentId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với ID: " + apartmentId));

                ResidentApartment headContract = residentApartmentRepository
                                .findByApartmentIdAndIsHeadTrueAndIsActiveTrue(apartmentId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Phòng này chưa có người thuê/chủ hộ, không thể thêm thành viên ở ghép!"));

                Resident resident = Resident.builder()
                                .fullName(request.getFullName())
                                .dob(request.getDob())
                                .phone(request.getPhone())
                                .citizenIdentity(request.getCitizenIdentity())
                                .email(request.getEmail())
                                .build();
                Resident savedResident = residentRepository.save(resident);

                ResidentApartment memberRecord = ResidentApartment.builder()
                                .resident(savedResident)
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
                                .id(savedResident.getId())
                                .fullName(savedResident.getFullName())
                                .citizenIdentity(savedResident.getCitizenIdentity())
                                .dob(savedResident.getDob())
                                .phone(savedResident.getPhone())
                                .email(savedResident.getEmail())
                                .build();
        }

        @Override
        @Transactional
        public ResidentResponse updateResident(Long residentId, UpdateResidentRequest request) {
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

                return ResidentResponse.builder()
                                .id(updatedResident.getId())
                                .fullName(updatedResident.getFullName())
                                .citizenIdentity(updatedResident.getCitizenIdentity())
                                .dob(updatedResident.getDob())
                                .phone(updatedResident.getPhone())
                                .email(updatedResident.getEmail())
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public Page<ResidentListResponse> getActiveResidents(String roomNumber, Pageable pageable) {
                Page<ResidentApartment> pageData;

                if (roomNumber != null && !roomNumber.isBlank()) {
                        pageData = residentApartmentRepository
                                        .findByApartment_RoomNumberContainingIgnoreCaseAndIsActiveTrue(roomNumber,
                                                        pageable);
                } else {
                        pageData = residentApartmentRepository.findByIsActiveTrue(pageable);
                }

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
                ResidentApartment ra = residentApartmentRepository
                                .findByResidentIdAndApartmentIdAndIsActiveTrue(residentId, apartmentId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Không tìm thấy thông tin cư trú hợp lệ để chuyển đi!"));

                ra.setIsActive(false);
                ra.setContractEnd(LocalDate.now());
                residentApartmentRepository.save(ra);

                if ("TENANT".equals(ra.getRole())) {
                        List<ResidentApartment> remainingMembers = residentApartmentRepository
                                        .findByApartmentIdAndIsActiveTrue(apartmentId);
                        for (ResidentApartment member : remainingMembers) {
                                member.setIsActive(false);
                                member.setContractEnd(LocalDate.now());
                        }
                        residentApartmentRepository.saveAll(remainingMembers);
                }

                long activeCount = residentApartmentRepository.countByApartmentIdAndIsActiveTrue(apartmentId);
                if (activeCount == 0) {
                        Apartment apartment = ra.getApartment();
                        apartment.setStatus("AVAILABLE");
                        apartmentRepository.save(apartment);
                }

                boolean stillHasOtherApartment = residentApartmentRepository
                                .existsByResidentIdAndIsActiveTrue(residentId);
                if (!stillHasOtherApartment) {
                        Resident resident = ra.getResident();
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

}
