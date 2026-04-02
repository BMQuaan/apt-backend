package com.ptithcm.apt.service.impl;

import com.ptithcm.apt.dto.response.FamilyMemberResponse;
import com.ptithcm.apt.dto.response.ProfileApartmentResponse;
import com.ptithcm.apt.dto.response.ProfileDashboardResponse;
import com.ptithcm.apt.dto.response.ProfileInfoResponse;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.repository.ResidentRepository;
import com.ptithcm.apt.repository.UserRepository;
import com.ptithcm.apt.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final ResidentRepository residentRepository;
    private final ResidentApartmentRepository residentApartmentRepository;

    private Resident getCurrentResident() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản người dùng"));

        return residentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new NotFoundException("Tài khoản chưa được liên kết với hồ sơ cư dân nào"));
    }

    @Override
    public ProfileDashboardResponse getProfileDashboard() {
        Resident resident = getCurrentResident();
        List<ResidentApartment> activeApartments = residentApartmentRepository.findByResident_IdAndIsActiveTrue(resident.getId());

        return ProfileDashboardResponse.builder()
                .personalInfo(buildProfileInfo(resident))
                .livingApartments(extractLivingApartments(activeApartments))
                .ownedApartments(extractOwnedApartments(activeApartments))
                .familyMembers(extractFamilyMembers(resident, activeApartments))
                .build();
    }

    // =========================================================================
    @Override
    public ProfileInfoResponse getMyProfile() {
        return buildProfileInfo(getCurrentResident());
    }

    @Override
    public List<ProfileApartmentResponse> getLivingApartments() {
        Resident resident = getCurrentResident();
        List<ResidentApartment> activeApartments = residentApartmentRepository.findByResident_IdAndIsActiveTrue(resident.getId());
        return extractLivingApartments(activeApartments);
    }

    @Override
    public List<ProfileApartmentResponse> getOwnedApartments() {
        Resident resident = getCurrentResident();
        List<ResidentApartment> activeApartments = residentApartmentRepository.findByResident_IdAndIsActiveTrue(resident.getId());
        return extractOwnedApartments(activeApartments);
    }

    @Override
    public List<FamilyMemberResponse> getFamilyMembers() {
        Resident resident = getCurrentResident();
        List<ResidentApartment> activeApartments = residentApartmentRepository.findByResident_IdAndIsActiveTrue(resident.getId());
        return extractFamilyMembers(resident, activeApartments);
    }

    // =========================================================================
    // CÁC HÀM HELPER NỘI BỘ
    // =========================================================================

    private ProfileInfoResponse buildProfileInfo(Resident resident) {
        return ProfileInfoResponse.builder()
                .residentId(resident.getId())
                .fullName(resident.getFullName())
                .citizenIdentity(resident.getCitizenIdentity())
                .phone(resident.getPhone())
                .email(resident.getEmail())
                .dob(resident.getDob())
                .build();
    }

    private List<ProfileApartmentResponse> extractLivingApartments(List<ResidentApartment> activeApartments) {
        return activeApartments.stream()
                .filter(ra -> "TENANT".equals(ra.getRole()) || "MEMBER".equals(ra.getRole()) || ("OWNER".equals(ra.getRole()) && ra.getIsHead()))
                .map(this::mapToApartmentResponse)
                .toList();
    }

    private List<ProfileApartmentResponse> extractOwnedApartments(List<ResidentApartment> activeApartments) {
        return activeApartments.stream()
                .filter(ra -> "OWNER".equals(ra.getRole()))
                .map(this::mapToApartmentResponse)
                .toList();
    }

    private List<FamilyMemberResponse> extractFamilyMembers(Resident resident, List<ResidentApartment> myActiveLinks) {
        List<Long> myLivingApartmentIds = myActiveLinks.stream()
                .filter(ra -> "TENANT".equals(ra.getRole()) || "MEMBER".equals(ra.getRole()) || ra.getIsHead())
                .map(ra -> ra.getApartment().getId())
                .toList();

        List<FamilyMemberResponse> familyMembers = new ArrayList<>();

        for (Long aptId : myLivingApartmentIds) {
            List<ResidentApartment> aptResidents = residentApartmentRepository.findByApartment_IdAndIsActiveTrue(aptId);

            for (ResidentApartment aptRes : aptResidents) {
                if (aptRes.getResident().getId().equals(resident.getId())) continue;

                boolean isAlreadyAdded = familyMembers.stream()
                        .anyMatch(member -> member.residentId().equals(aptRes.getResident().getId()));

                if (!isAlreadyAdded) {
                    familyMembers.add(FamilyMemberResponse.builder()
                            .residentId(aptRes.getResident().getId())
                            .fullName(aptRes.getResident().getFullName())
                            .phone(aptRes.getResident().getPhone())
                            .dob(aptRes.getResident().getDob())
                            .role(aptRes.getRole())
                            .isHead(aptRes.getIsHead())
                            .build());
                }
            }
        }
        return familyMembers;
    }

    private ProfileApartmentResponse mapToApartmentResponse(ResidentApartment ra) {
        return ProfileApartmentResponse.builder()
                .apartmentId(ra.getApartment().getId())
                .roomNumber(ra.getApartment().getRoomNumber())
                .floor(ra.getApartment().getFloor())
                .area(ra.getApartment().getArea())
                .role(ra.getRole())
                .isHead(ra.getIsHead())
                .contractStart(ra.getContractStart())
                .contractEnd(ra.getContractEnd())
                .build();
    }
}