package com.ptithcm.apt.service.impl;

import java.time.format.DateTimeFormatter;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ptithcm.apt.dto.request.ContractRequest;
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

    @Override
    @Transactional
    public ResidentResponse createContract(ContractRequest request) {

        if (residentRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email này đã tồn tại trong hệ thống!");
        }

        if (residentRepository.existsByCitizenIdentity(request.getCitizenIdentity())) {
            throw new RuntimeException("Căn cước công dân này đã tồn tại trong hệ thống!");
        }
        if (!"TENANT".equals(request.getRole()) && !"OWNER".equals(request.getRole())) {
            throw new RuntimeException("Vai trò khi lập hợp đồng chỉ có thể là TENANT hoặc OWNER");
        }

        if (request.getContractEnd() != null && request.getContractStart().isAfter(request.getContractEnd())) {
            throw new RuntimeException("Ngày bắt đầu hợp đồng không thể nằm sau ngày kết thúc!");
        }

        Apartment apartment = apartmentRepository.findById(request.getApartmentId())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy phòng với ID: " + request.getApartmentId()));

        if (!"AVAILABLE".equals(apartment.getStatus())) {
            throw new RuntimeException("Phòng này hiện không trống, không thể lập hợp đồng mới!");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        String rawPassword = request.getDob().format(formatter);

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

        Resident resident = Resident.builder()
                .user(savedUser)
                .fullName(request.getFullName())
                .dob(request.getDob())
                .phone(request.getPhone())
                .citizenIdentity(request.getCitizenIdentity())
                .email(request.getEmail())
                .build();
        Resident savedResident = residentRepository.save(resident);

        ResidentApartment contract = ResidentApartment.builder()
                .resident(savedResident)
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

        return ResidentResponse.builder()
                .id(savedResident.getId())
                .fullName(savedResident.getFullName())
                .citizenIdentity(savedResident.getCitizenIdentity())
                .dob(savedResident.getDob())
                .phone(savedResident.getPhone())
                .email(savedResident.getEmail())
                .build();
    }
}
