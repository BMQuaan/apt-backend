package com.ptithcm.apt.service.impl;

import com.ptithcm.apt.dto.request.ResidentRequest;
import com.ptithcm.apt.dto.request.UpdateResidentRequest;
import com.ptithcm.apt.dto.response.ResidentListResponse;
import com.ptithcm.apt.dto.response.ResidentResponse;
import com.ptithcm.apt.entity.*;
import com.ptithcm.apt.repository.*;
import com.ptithcm.apt.service.ResidentService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Override
    @Transactional
    public void createResidentAndAssignApartment(ResidentRequest request) {

        // 1. Kiểm tra CCCD đã tồn tại chưa
        if (residentRepository.existsByCitizenIdentity(request.getCitizenIdentity())) {
            throw new RuntimeException("Căn cước công dân này đã tồn tại trong hệ thống!");
        }

        Apartment apartment = apartmentRepository.findById(request.getApartmentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy căn hộ với ID: " + request.getApartmentId()));

        if ("TENANT".equals(request.getRole())) {
            boolean hasTenant = residentApartmentRepository
                    .existsByApartmentIdAndRoleAndIsActiveTrue(request.getApartmentId(), "TENANT");
            if (hasTenant) {
                throw new RuntimeException(
                        "Căn hộ này đã có Người thuê chính (TENANT). Vui lòng thêm người này với vai trò Thành viên (MEMBER)!");
            }
            apartment.setStatus("RENTED");
        } else if ("OWNER".equals(request.getRole())) {
            apartment.setStatus("OWNED");
        } else if ("MEMBER".equals(request.getRole())) {
            if ("AVAILABLE".equals(apartment.getStatus())) {
                throw new RuntimeException(
                        "Không thể thêm Thành viên vào một căn hộ đang trống. Vui lòng thêm Chủ hộ hoặc Người thuê chính trước!");
            }
        }

        if (Boolean.TRUE.equals(request.getIsHead())) {
            boolean hasHead = residentApartmentRepository
                    .existsByApartmentIdAndIsHeadTrueAndIsActiveTrue(request.getApartmentId());
            if (hasHead) {
                throw new RuntimeException("Căn hộ này đã có Chủ hộ đại diện nhận hóa đơn. Không thể chọn thêm!");
            }
        }

        // Tao mk mac dinh ngay sinh
        String defaultPassword = "defaultPassword";
        if (request.getDob() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
            defaultPassword = request.getDob().format(formatter);
        }

        // Tạo tài khoản User
        Role userRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new RuntimeException("Chưa cấu hình Role USER trong DB"));

        User newUser = User.builder()
                .username(request.getCitizenIdentity()) // Dùng CCCD làm tên đăng nhập
                .password(passwordEncoder.encode(defaultPassword)) // Mật khẩu mặc định
                .role(userRole)
                .isActive(true)
                .build();
        User savedUser = userRepository.save(newUser);

        // Tạo thông tin cá nhân (Resident)
        Resident newResident = Resident.builder()
                .user(savedUser)
                .fullName(request.getFullName())
                .citizenIdentity(request.getCitizenIdentity())
                .phone(request.getPhone())
                .email(request.getEmail())
                .dob(request.getDob())
                .build();
        Resident savedResident = residentRepository.save(newResident);

        // Gán cư dân vào căn hộ (Ghi vào bảng trung gian resident_apartments)
        ResidentApartment assignment = ResidentApartment.builder()
                .resident(savedResident)
                .apartment(apartment)
                .role(request.getRole()) // OWNER, TENANT, hoặc MEMBER
                .isHead(request.getIsHead() != null ? request.getIsHead() : false)
                .isActive(true)
                .contractStart(request.getContractStart())
                .contractEnd(request.getContractEnd())
                .build();
        residentApartmentRepository.save(assignment);
        apartmentRepository.save(apartment);
    }

    @Override
    @Transactional
    public ResidentResponse updateResident(Long residentId, UpdateResidentRequest request) {
        // 1. Tìm cư dân
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân!"));

        // 2. Cập nhật các trường cho phép (Thông tin liên lạc & cá nhân)
        resident.setFullName(request.getFullName());
        resident.setPhone(request.getPhone());
        resident.setEmail(request.getEmail());
        resident.setDob(request.getDob());

        // 3. Lưu vào DB
        Resident updatedResident = residentRepository.save(resident);

        // 4. Trả về toàn bộ thông tin cá nhân mới nhất
        return ResidentResponse.builder()
                .id(updatedResident.getId())
                .fullName(updatedResident.getFullName())
                .citizenIdentity(updatedResident.getCitizenIdentity()) // Trả về CCCD cũ
                .dob(updatedResident.getDob())
                .phone(updatedResident.getPhone())
                .email(updatedResident.getEmail())
                .build();
    }

    // === TRONG ResidentServiceImpl.java ===

    // 1. LẤY DANH SÁCH (Có phân trang và lọc theo phòng)
    @Override
    @Transactional(readOnly = true)
    public Page<ResidentListResponse> getActiveResidents(String roomNumber, Pageable pageable) {
        Page<ResidentApartment> pageData;

        if (roomNumber != null && !roomNumber.isBlank()) {
            pageData = residentApartmentRepository
                    .findByApartment_RoomNumberContainingIgnoreCaseAndIsActiveTrue(roomNumber, pageable);
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

    // XÓA/CHUYỂN PHÒNG (Move Out)
    @Override
    @Transactional
    public void moveOutResident(Long residentId, Long apartmentId) {
        // Lấy bản ghi cư trú đang active
        ResidentApartment ra = residentApartmentRepository
                .findByResidentIdAndApartmentIdAndIsActiveTrue(residentId, apartmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin cư trú hợp lệ để chuyển đi!"));

        // Cập nhật trạng thái thành false (đã chuyển đi) và chốt ngày kết thúc
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

        // Logic cập nhật trạng thái phòng:
        // Nếu đếm số lượng người ĐANG Ở trong phòng này = 0 -> Đổi status phòng thành
        // AVAILABLE
        long activeCount = residentApartmentRepository.countByApartmentIdAndIsActiveTrue(apartmentId);
        if (activeCount == 0) {
            Apartment apartment = ra.getApartment();
            apartment.setStatus("AVAILABLE");
            apartmentRepository.save(apartment);
        }

        boolean stillHasOtherApartment = residentApartmentRepository.existsByResidentIdAndIsActiveTrue(residentId);
        if (!stillHasOtherApartment) {
            Resident resident = ra.getResident();
            User user = resident.getUser();
            if (user != null && user.getIsActive()) {
                user.setIsActive(false);
                userRepository.save(user);
            }
        }
    }
}
