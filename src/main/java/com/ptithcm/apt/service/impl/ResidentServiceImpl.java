package com.ptithcm.apt.service.impl;

import com.ptithcm.apt.dto.request.ContractRequest;
import com.ptithcm.apt.dto.request.MemberRequest;
import com.ptithcm.apt.dto.request.UpdateResidentRequest;
import com.ptithcm.apt.dto.response.MyApartmentResponse;
import com.ptithcm.apt.dto.response.ResidentDetailResponse;
import com.ptithcm.apt.dto.response.ResidentListResponse;
import com.ptithcm.apt.dto.response.ResidentResponse;
import com.ptithcm.apt.entity.*;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.enums.RentStatus;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.repository.*;
import com.ptithcm.apt.service.ResidentService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        private final RentInvoiceRepository rentInvoiceRepository;

        private static final String ROLE_MEMBER = "MEMBER";
        private static final String ROLE_OWNER = "OWNER";
        private static final String ROLE_TENANT = "TENANT";
        private static final String ROLE_ADMIN = "ADMIN";
        private static final String ROLE_ADMIN_SPRING = "ROLE_ADMIN";
        private static final String STATUS_OWNED = "OWNED";
        private static final String STATUS_AVAILABLE = "AVAILABLE";

        @Override
        public Optional<String> findNameByUserId(Long userId) {
                return residentRepository.findByUser_Id(userId).map(Resident::getFullName);
        }

        @Override
        @Transactional
        public ResidentResponse addMemberToApartment(String roomNumber, MemberRequest request) {
                Apartment apartment = getApartmentByRoomNumber(roomNumber);
                ResidentApartment headContract = getHeadContractForApartment(apartment.getId());
                Resident resident = getOrCreateMemberResident(request);

                createMemberResidencyRecord(resident, apartment, headContract);

                return buildResidentResponse(resident);
        }

        @Override
        @Transactional(readOnly = true)
        public ResidentDetailResponse getResidentDetailById(Long residentId) {
                Resident resident = getResidentById(residentId);
                List<ResidentApartment> activeRas = residentApartmentRepository
                                .findAllByResidentIdAndIsActiveTrue(residentId);
                List<ResidentDetailResponse.ResidencyInfo> residencies = mapToResidencyInfos(activeRas);

                return buildResidentDetailResponse(resident, residencies);
        }

        @Override
        @Transactional
        public ResidentDetailResponse updateResident(Long residentId, UpdateResidentRequest request) {
                Resident resident = getResidentById(residentId);

                updateBasicInfo(resident, request);
                updateCitizenIdentity(resident, request.getCitizenIdentity());
                updateEmailAndUserAccount(resident, request.getEmail());

                Resident updatedResident = residentRepository.save(resident);
                return getResidentDetailById(updatedResident.getId());
        }

        @Override
        @Transactional(readOnly = true)
        public Page<ResidentListResponse> getActiveResidents(String keyword, Pageable pageable) {
                Page<ResidentApartment> pageData = residentApartmentRepository.searchAllActiveResidents(keyword,
                                pageable);
                return pageData.map(this::mapToResidentListResponse);
        }

        @Override
        @Transactional
        public void moveOutResident(Long residentId, Long apartmentId) {
                validateNoUnpaidBills(apartmentId);
                ResidentApartment ra = getValidResidencyRecord(residentId, apartmentId);

                if (ra.getIsHead()) {
                        processHeadMovingOut(ra, apartmentId);
                } else {
                        // Nếu người ở ghép (MEMBER) dọn đi -> XÓA SẠCH người này
                        completelyRemoveResidentData(ra.getResident(), apartmentId);
                }
        }

        @Override
        @Transactional(readOnly = true)
        public List<ResidentListResponse> getResidentsByApartment(Long apartmentId) {
                validateUserAccessToApartment(apartmentId);

                if (!apartmentRepository.existsById(apartmentId)) {
                        throw new RuntimeException("Không tìm thấy căn hộ với ID:" + apartmentId);
                }

                List<ResidentApartment> list = residentApartmentRepository
                                .findByApartmentIdAndIsActiveTrue(apartmentId);
                return list.stream().map(this::mapToResidentListResponse).collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<MyApartmentResponse> getMyApartments() {
                Resident currentResident = getCurrentResidentFromSecurityContext();

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

        @Override
        @Transactional(readOnly = true)
        public Resident findByUserId(Long userId) {
                return residentRepository.findByUser_Id(userId)
                                .orElseThrow(() -> new NotFoundException(
                                                "Tài khoản chưa được liên kết với hồ sơ cư dân nào"));
        }

        @Override
        public Optional<Resident> findById(Long userId) {
                return residentRepository.findById(userId);
        }

        @Override
        @Transactional(readOnly = true)
        public Optional<ResidentResponse> checkResidentByCccd(String cccd) {
                return residentRepository.findByCitizenIdentity(cccd).map(this::buildResidentResponse);
        }

        // ============================================================================

        private Apartment getApartmentByRoomNumber(String roomNumber) {
                return apartmentRepository.findByRoomNumber(roomNumber)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với ID: " + roomNumber));
        }

        private ResidentApartment getHeadContractForApartment(Long apartmentId) {
                return residentApartmentRepository.findByApartmentIdAndIsHeadTrueAndIsActiveTrue(apartmentId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Phòng này chưa có người thuê/chủ hộ, không thể thêm thành viên ở ghép!"));
        }

        private Resident getOrCreateMemberResident(MemberRequest request) {
                var existingByEmail = residentRepository.findByEmail(request.getEmail());
                var existingByCccd = residentRepository.findByCitizenIdentity(request.getCitizenIdentity());

                if (existingByEmail.isPresent()) {
                        Resident resident = existingByEmail.get();
                        if (!resident.getCitizenIdentity().equals(request.getCitizenIdentity())) {
                                throw new RuntimeException("Email này đã được đăng ký cho một CCCD khác!");
                        }
                        return resident;
                } else if (existingByCccd.isPresent()) {
                        throw new RuntimeException("Căn cước công dân này đã tồn tại với Email khác!");
                }

                Resident newResident = Resident.builder()
                                .fullName(request.getFullName())
                                .dob(request.getDob())
                                .phone(request.getPhone())
                                .citizenIdentity(request.getCitizenIdentity())
                                .email(request.getEmail())
                                .build();
                return residentRepository.save(newResident);
        }

        private void createMemberResidencyRecord(Resident resident, Apartment apartment,
                        ResidentApartment headContract) {
                ResidentApartment memberRecord = ResidentApartment.builder()
                                .resident(resident)
                                .apartment(apartment)
                                .role(ROLE_MEMBER)
                                .isHead(false)
                                .isActive(true)
                                .rentalPrice(BigDecimal.ZERO)
                                .depositAmount(BigDecimal.ZERO)
                                .contractStart(headContract.getContractStart())
                                .contractEnd(headContract.getContractEnd())
                                .build();
                residentApartmentRepository.save(memberRecord);
        }

        private Resident getResidentById(Long residentId) {
                return residentRepository.findById(residentId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy cư dân!"));
        }

        private void updateBasicInfo(Resident resident, UpdateResidentRequest request) {
                if (request.getFullName() != null && !request.getFullName().isBlank())
                        resident.setFullName(request.getFullName());
                if (request.getPhone() != null && !request.getPhone().isBlank())
                        resident.setPhone(request.getPhone());
                if (request.getDob() != null)
                        resident.setDob(request.getDob());
        }

        private void updateCitizenIdentity(Resident resident, String newCccd) {
                if (newCccd != null && !newCccd.isBlank() && !newCccd.equals(resident.getCitizenIdentity())) {
                        if (residentRepository.existsByCitizenIdentity(newCccd)) {
                                throw new RuntimeException("Căn cước công dân mới này đã tồn tại trong hệ thống!");
                        }
                        resident.setCitizenIdentity(newCccd);
                }
        }

        private void updateEmailAndUserAccount(Resident resident, String newEmail) {
                if (newEmail != null && !newEmail.isBlank() && !newEmail.equals(resident.getEmail())) {
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

        private void validateNoUnpaidBills(Long apartmentId) {
                if (billRepository.existsByApartmentIdAndStatus(apartmentId, BillStatus.UNPAID)) {
                        throw new RuntimeException("Không thể chuyển đi! Căn hộ này vẫn còn hóa đơn chưa thanh toán.");
                }
                if (rentInvoiceRepository.existsByApartmentIdAndStatus(apartmentId, RentStatus.UNPAID)) {
                        throw new RuntimeException(
                                        "Không thể chuyển đi! Căn hộ này vẫn còn hóa đơn TIỀN THUÊ NHÀ chưa thanh toán.");
                }
        }

        private ResidentApartment getValidResidencyRecord(Long residentId, Long apartmentId) {
                return residentApartmentRepository
                                .findByResidentIdAndApartmentIdAndIsActiveTrue(residentId, apartmentId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin cư trú hợp lệ!"));
        }

        private void processHeadMovingOut(ResidentApartment headRa, Long apartmentId) {
                List<ResidentApartment> allMembers = residentApartmentRepository
                                .findByApartmentIdAndIsActiveTrue(apartmentId);
                Optional<ResidentApartment> ownerOpt = allMembers.stream()
                                .filter(m -> ROLE_OWNER.equals(m.getRole()) && !m.getId().equals(headRa.getId()))
                                .findFirst();

                for (ResidentApartment member : allMembers) {
                        // Nếu Khách thuê (TENANT) dọn đi, KHÔNG ĐƯỢC XÓA Chủ nhà (OWNER)
                        if (ROLE_TENANT.equals(headRa.getRole()) && ROLE_OWNER.equals(member.getRole())) {
                                continue;
                        }
                        completelyRemoveResidentData(member.getResident(), apartmentId);
                }

                Apartment apartment = headRa.getApartment();
                if (ownerOpt.isPresent()) {
                        ResidentApartment owner = ownerOpt.get();
                        owner.setIsHead(true);
                        residentApartmentRepository.save(owner);
                        apartment.setStatus(STATUS_OWNED);
                } else {
                        apartment.setStatus(STATUS_AVAILABLE);
                }
                apartmentRepository.save(apartment);
        }

        private void completelyRemoveResidentData(Resident resident, Long apartmentId) {
                List<ResidentApartment> allContracts = residentApartmentRepository.findByResidentId(resident.getId());
                boolean isLivingInOtherRoom = allContracts.stream()
                                .anyMatch(c -> c.getIsActive() && !c.getApartment().getId().equals(apartmentId));

                if (!isLivingInOtherRoom) {
                        residentApartmentRepository.deleteAll(allContracts); // Xóa hợp đồng
                        User user = resident.getUser();
                        if (user != null) {
                                resident.setUser(null);
                                residentRepository.save(resident);
                                residentRepository.delete(resident);
                                userRepository.delete(user);
                        } else {
                                residentRepository.delete(resident);
                        }
                } else {
                        List<ResidentApartment> contractsInThisRoom = allContracts.stream()
                                        .filter(c -> c.getApartment().getId().equals(apartmentId))
                                        .collect(Collectors.toList());
                        residentApartmentRepository.deleteAll(contractsInThisRoom);
                }
        }

        private void validateUserAccessToApartment(Long apartmentId) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated()) {
                        boolean isAdmin = auth.getAuthorities().stream()
                                        .anyMatch(role -> role.getAuthority().equals(ROLE_ADMIN)
                                                        || role.getAuthority().equals(ROLE_ADMIN_SPRING));

                        if (!isAdmin) {
                                Resident currentResident = residentRepository.findByEmail(auth.getName())
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
        }

        private Resident getCurrentResidentFromSecurityContext() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated()) {
                        throw new RuntimeException("Chưa đăng nhập!");
                }
                return residentRepository.findByEmail(auth.getName())
                                .orElseThrow(() -> new RuntimeException(
                                                "Tài khoản của bạn chưa được liên kết với hồ sơ cư dân nào!"));
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

        private ResidentDetailResponse buildResidentDetailResponse(Resident resident,
                        List<ResidentDetailResponse.ResidencyInfo> residencies) {
                return ResidentDetailResponse.builder()
                                .id(resident.getId())
                                .fullName(resident.getFullName())
                                .citizenIdentity(resident.getCitizenIdentity())
                                .dob(resident.getDob() != null ? resident.getDob().toString() : null)
                                .phone(resident.getPhone())
                                .email(resident.getEmail())
                                .residencies(residencies)
                                .build();
        }

        private ResidentListResponse mapToResidentListResponse(ResidentApartment ra) {
                return ResidentListResponse.builder()
                                .residentId(ra.getResident().getId())
                                .fullName(ra.getResident().getFullName())
                                .citizenIdentity(ra.getResident().getCitizenIdentity())
                                .phone(ra.getResident().getPhone())
                                .roomNumber(ra.getApartment().getRoomNumber())
                                .role(ra.getRole())
                                .isHead(ra.getIsHead())
                                .contractStart(ra.getContractStart())
                                .build();
        }

        private List<ResidentDetailResponse.ResidencyInfo> mapToResidencyInfos(List<ResidentApartment> ras) {
                return ras.stream().map(ra -> ResidentDetailResponse.ResidencyInfo.builder()
                                .apartmentId(ra.getApartment().getId())
                                .roomNumber(ra.getApartment().getRoomNumber())
                                .role(ra.getRole())
                                .isHead(ra.getIsHead())
                                .build()).collect(Collectors.toList());
        }
}