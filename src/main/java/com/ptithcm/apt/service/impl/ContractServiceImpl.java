package com.ptithcm.apt.service.impl;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ptithcm.apt.dto.request.ContractRequest;
import com.ptithcm.apt.dto.response.ContractResponse;
import com.ptithcm.apt.dto.response.ResidentResponse;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.Role;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.repository.ApartmentRepository;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.repository.ResidentRepository;
import com.ptithcm.apt.repository.RoleRepository;
import com.ptithcm.apt.repository.UserRepository;
import com.ptithcm.apt.service.ContractService;
import com.ptithcm.apt.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {
    private final ResidentRepository residentRepository;
    private final ResidentApartmentRepository residentApartmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApartmentRepository apartmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String ROLE_TENANT = "TENANT";
    private static final String ROLE_OWNER = "OWNER";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_OWNED = "OWNED";
    private static final String STATUS_RENTED = "RENTED";

    @Override
    @Transactional
    public ResidentResponse createContract(ContractRequest request) {
        // 1. Kiểm tra tính hợp lệ của dữ liệu đầu vào (Validation)
        validateContractRequest(request);

        // 2. Lấy thông tin phòng và kiểm tra trạng thái phòng
        Apartment apartment = getAndValidateApartment(request.getApartmentId(), request.getRole());

        // 3. Xử lý chủ sở hữu cũ (nếu người mới là TENANT thuê căn hộ đã có chủ)
        handleExistingOwnerIfNecessary(apartment, request.getRole());

        // 4. Lấy hoặc tạo mới Cư dân (Resident) & tài khoản User
        ResidentAccountResult residentResult = getOrCreateResident(request);
        Resident resident = residentResult.getResident();

        // 5. Tạo hợp đồng (ResidentApartment)
        createResidentApartment(request, resident, apartment);

        // 6. Cập nhật trạng thái phòng dựa theo vai trò hợp đồng
        updateApartmentStatus(apartment, request.getRole());

        // 7. Gửi email nếu hệ thống vừa tạo tài khoản mới
        if (residentResult.isNewAccount()) {
            sendWelcomeEmail(resident, apartment, residentResult.getRawPassword());
        }

        return buildResidentResponse(resident);
    }

    private void validateContractRequest(ContractRequest request) {
        if (request.getDob() == null) {
            throw new RuntimeException("Ngày sinh không được để trống!");
        }

        int age = Period.between(request.getDob(), LocalDate.now()).getYears();
        if (age < 18) {
            throw new RuntimeException("Người đại diện lập hợp đồng phải từ đủ 18 tuổi trở lên!");
        }

        if (!ROLE_TENANT.equals(request.getRole()) && !ROLE_OWNER.equals(request.getRole())) {
            throw new RuntimeException("Vai trò khi lập hợp đồng chỉ có thể là TENANT hoặc OWNER");
        }

        if (request.getContractEnd() != null && request.getContractStart().isAfter(request.getContractEnd())) {
            throw new RuntimeException("Ngày bắt đầu hợp đồng không thể nằm sau ngày kết thúc!");
        }
    }

    private Apartment getAndValidateApartment(Long apartmentId, String role) {
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với ID: " + apartmentId));

        String status = apartment.getStatus();
        if (ROLE_OWNER.equals(role) && !STATUS_AVAILABLE.equals(status)) {
            throw new RuntimeException("Chỉ có thể lập hợp đồng OWNER cho một căn hộ đang trống!");
        }

        if (ROLE_TENANT.equals(role) && !STATUS_AVAILABLE.equals(status) && !STATUS_OWNED.equals(status)) {
            throw new RuntimeException("Chỉ có thể thuê căn hộ đang trống hoặc căn hộ đã có chủ sở hữu!");
        }
        return apartment;
    }

    private void handleExistingOwnerIfNecessary(Apartment apartment, String role) {
        if (ROLE_TENANT.equals(role) && STATUS_OWNED.equals(apartment.getStatus())) {
            residentApartmentRepository.findByApartmentIdAndIsHeadTrueAndIsActiveTrue(apartment.getId())
                    .ifPresent(currentOwner -> {
                        currentOwner.setIsHead(false);
                        residentApartmentRepository.save(currentOwner); // Cuối hàm @Transactional sẽ tự động commit
                                                                        // xuống DB
                    });
        }
    }

    private ResidentAccountResult getOrCreateResident(ContractRequest request) {
        var existingResidentByEmail = residentRepository.findByEmail(request.getEmail());
        var existingResidentByCccd = residentRepository.findByCitizenIdentity(request.getCitizenIdentity());

        if (existingResidentByEmail.isPresent()) {
            Resident resident = existingResidentByEmail.get();
            if (!resident.getCitizenIdentity().equals(request.getCitizenIdentity())) {
                throw new RuntimeException("Email này đã được đăng ký cho một CCCD khác!");
            }
            return new ResidentAccountResult(resident, false, null);
        }

        if (existingResidentByCccd.isPresent()) {
            throw new RuntimeException("CCCD này đã tồn tại nhưng đăng ký với một Email khác!");
        }

        // Tạo tài khoản mới hoàn toàn
        String rawPassword = request.getDob().format(DateTimeFormatter.ofPattern("ddMMyyyy"));

        Role userRole = roleRepository.findByRoleName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Chưa cấu hình Role USER trong DB"));

        User newUser = User.builder()
                .username(request.getEmail())
                .password(passwordEncoder.encode(rawPassword))
                .role(userRole)
                .isActive(true)
                .build();
        User savedUser = userRepository.save(newUser);

        Resident newResident = Resident.builder()
                .user(savedUser)
                .fullName(request.getFullName())
                .dob(request.getDob())
                .phone(request.getPhone())
                .citizenIdentity(request.getCitizenIdentity())
                .email(request.getEmail())
                .build();

        return new ResidentAccountResult(residentRepository.save(newResident), true, rawPassword);
    }

    private void createResidentApartment(ContractRequest request, Resident resident, Apartment apartment) {
        ResidentApartment contract = ResidentApartment.builder()
                .resident(resident)
                .apartment(apartment)
                .role(request.getRole())
                .isHead(true)
                .isActive(true)
                .rentalPrice(request.getRentalPrice())
                .depositAmount(request.getDepositAmount())
                .contractStart(request.getContractStart())
                .contractEnd(request.getContractEnd())
                .build();
        residentApartmentRepository.save(contract);
    }

    private void updateApartmentStatus(Apartment apartment, String role) {
        if (STATUS_OWNED.equals(apartment.getStatus())) {
            apartment.setStatus(STATUS_OWNED);
        } else {
            apartment.setStatus(ROLE_TENANT.equals(role) ? STATUS_RENTED : STATUS_OWNED);
        }
        apartmentRepository.save(apartment);
    }

    private void sendWelcomeEmail(Resident resident, Apartment apartment, String rawPassword) {
        try {
            Map<String, String> templateModel = Map.of(
                    "FULL_NAME", resident.getFullName(),
                    "ROOM_NUMBER", apartment.getRoomNumber(),
                    "EMAIL", resident.getEmail(),
                    "PASSWORD", rawPassword);

            emailService.sendHtmlEmail(
                    resident.getEmail(),
                    "Chào mừng đến với APT - Thông tin tài khoản cư dân",
                    "welcome-email.html",
                    templateModel);
        } catch (Exception e) {
            log.error("Cảnh báo: Tạo hợp đồng thành công nhưng không thể gửi email. Lỗi: {}", e.getMessage());
        }
    }

    private ResidentResponse buildResidentResponse(Resident resident) {
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
    @Transactional
    public Page<ContractResponse> getAllContracts(String keyword, String role, Pageable pageable) {
        Page<ResidentApartment> pageData = residentApartmentRepository.searchAndFilterContracts(keyword, role,
                pageable);
        return pageData.map(ra -> mapToContractResponse(ra));
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getContractDetail(Long contractId) {
        ResidentApartment contract = residentApartmentRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId));
        return mapToContractResponse(contract);
    }

    private ContractResponse mapToContractResponse(ResidentApartment ra) {
        return ContractResponse.builder()
                .id(ra.getId())
                .residentId(ra.getResident().getId())
                .apartmentId(ra.getApartment().getId())
                .roomNumber(ra.getApartment().getRoomNumber())
                .residentName(ra.getResident().getFullName())
                .citizenIdentity(ra.getResident().getCitizenIdentity())
                .phone(ra.getResident().getPhone())
                .role(ra.getRole())
                .isHead(ra.getIsHead())
                .rentalPrice(ra.getRentalPrice())
                .depositAmount(ra.getDepositAmount())
                .contractStart(ra.getContractStart())
                .contractEnd(ra.getContractEnd())
                .isActive(ra.getIsActive())
                .build();
    }

    // Class phụ trợ (Lồng bên trong - Inner Class) để trả về nhiều tham số khi tạo
    // resident
    @lombok.Getter
    @lombok.AllArgsConstructor
    private static class ResidentAccountResult {
        private Resident resident;
        private boolean isNewAccount;
        private String rawPassword;
    }
}