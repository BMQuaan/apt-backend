package com.ptithcm.apt.service.impl;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    @Transactional
    public ResidentResponse createContract(ContractRequest request) {

        if (!"TENANT".equals(request.getRole()) && !"OWNER".equals(request.getRole())) {
            throw new RuntimeException("Vai trò khi lập hợp đồng chỉ có thể là TENANT hoặc OWNER");
        }

        if (request.getContractEnd() != null && request.getContractStart().isAfter(request.getContractEnd())) {
            throw new RuntimeException("Ngày bắt đầu hợp đồng không thể nằm sau ngày kết thúc!");
        }

        Apartment apartment = apartmentRepository.findById(request.getApartmentId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy phòng với ID: " + request.getApartmentId()));

        if ("OWNER".equals(request.getRole()) && !"AVAILABLE".equals(apartment.getStatus())) {
            throw new RuntimeException("Chỉ có thể lập hợp đồng OWNER cho một căn hộ đang trống!");
        }

        if ("TENANT".equals(request.getRole()) && !"AVAILABLE".equals(apartment.getStatus())
                && !"OWNED".equals(apartment.getStatus())) {
            throw new RuntimeException("Chỉ có thể thuê căn hộ đang trống hoặc căn hộ đã có chủ sở hữu!");
        }

        if ("TENANT".equals(request.getRole()) && "OWNED".equals(apartment.getStatus())) {
            residentApartmentRepository.findByApartmentIdAndIsHeadTrueAndIsActiveTrue(apartment.getId())
                    .ifPresent(currentOwner -> {

                        currentOwner.setIsHead(false);
                        residentApartmentRepository.saveAndFlush(currentOwner);
                    });
        }

        Resident resident;
        boolean isNewAccount = false;
        String rawPassword = "";

        var existingResidentByEmail = residentRepository.findByEmail(request.getEmail());
        var existingResidentByCccd = residentRepository.findByCitizenIdentity(request.getCitizenIdentity());

        if (existingResidentByEmail.isPresent()) {
            resident = existingResidentByEmail.get();
            if (!resident.getCitizenIdentity().equals(request.getCitizenIdentity())) {
                throw new RuntimeException("Email này đã được đăng ký cho một CCCD khác!");
            }
        } else if (existingResidentByCccd.isPresent()) {
            throw new RuntimeException("CCCD này đã tồn tại nhưng đăng ký với một Email khác!");
        } else {
            isNewAccount = true;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
            rawPassword = request.getDob().format(formatter);

            // Tạo tài khoản User
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
            resident = residentRepository.save(newResident);

        }

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

        apartment.setStatus("TENANT".equals(request.getRole()) ? "RENTED" : "OWNED");
        apartmentRepository.save(apartment);

        if (isNewAccount) {
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
                System.err
                        .println("Cảnh báo: Tạo hợp đồng thành công nhưng không thể gửi email. Lỗi: " + e.getMessage());
            }
        }

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
        Page<ResidentApartment> pageData;

        pageData = residentApartmentRepository.searchAndFilterContracts(keyword, role,
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
}
