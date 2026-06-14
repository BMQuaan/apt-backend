package com.ptithcm.apt.service.impl;

import com.ptithcm.apt.dto.response.FamilyMemberResponse;
import com.ptithcm.apt.dto.response.ProfileApartmentResponse;
import com.ptithcm.apt.dto.response.ProfileDashboardResponse;
import com.ptithcm.apt.dto.response.ProfileInfoResponse;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.service.ProfileService;
import com.ptithcm.apt.service.ResidentApartmentService;
import com.ptithcm.apt.service.ResidentService;
import com.ptithcm.apt.service.UserService;
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

    private final UserService userService;
    private final ResidentService residentService;
    private final ResidentApartmentService residentApartmentService;

    private Resident getCurrentResident() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userService.findByUsername(username);
        return residentService.findByUserId(user.getId());
    }

    // =========================================================================

    @Override
    public ProfileDashboardResponse getProfileDashboard() {
        Resident resident = getCurrentResident();
        List<ResidentApartment> activeApartments = residentApartmentService.findActiveByResidentId(resident.getId());

        return ProfileDashboardResponse.builder()
                .personalInfo(buildProfileInfo(resident))
                .livingApartment(extractLivingApartment(activeApartments))
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
    public ProfileApartmentResponse getLivingApartment() {
        Resident resident = getCurrentResident();
        List<ResidentApartment> activeApartments = residentApartmentService.findActiveByResidentId(resident.getId());
        return extractLivingApartment(activeApartments);
    }

    @Override
    public List<ProfileApartmentResponse> getOwnedApartments() {
        Resident resident = getCurrentResident();
        List<ResidentApartment> activeApartments = residentApartmentService.findActiveByResidentId(resident.getId());
        return extractOwnedApartments(activeApartments);
    }

    @Override
    public List<FamilyMemberResponse> getFamilyMembers() {
        Resident resident = getCurrentResident();
        List<ResidentApartment> activeApartments = residentApartmentService.findActiveByResidentId(resident.getId());
        return extractFamilyMembers(resident, activeApartments);
    }

    // =========================================================================
    // HELPER METHODS (build / extract)
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

    private ProfileApartmentResponse extractLivingApartment(List<ResidentApartment> activeApartments) {
        return activeApartments.stream()
                .filter(ra -> "TENANT".equals(ra.getRole())
                        || "MEMBER".equals(ra.getRole())
                        || ("OWNER".equals(ra.getRole()) && ra.getIsHead()))
                .findFirst()
                .map(this::mapToApartmentResponse)
                .orElse(null);
    }

    private List<ProfileApartmentResponse> extractOwnedApartments(List<ResidentApartment> activeApartments) {
        return activeApartments.stream()
                .filter(ra -> "OWNER".equals(ra.getRole()))
                .map(this::mapToApartmentResponse)
                .toList();
    }

    private List<FamilyMemberResponse> extractFamilyMembers(Resident resident,
            List<ResidentApartment> myActiveLinks) {
        ResidentApartment myLivingLink = myActiveLinks.stream()
                .filter(ra -> "TENANT".equals(ra.getRole())
                        || "MEMBER".equals(ra.getRole())
                        || ra.getIsHead())
                .findFirst()
                .orElse(null);

        if (myLivingLink == null) {
            return List.of();
        }

        Long livingAptId = myLivingLink.getApartment().getId();
        String myRole = myLivingLink.getRole();

        List<FamilyMemberResponse> familyMembers = new ArrayList<>();
        List<ResidentApartment> aptResidents = residentApartmentService.findActiveByApartmentId(livingAptId);

        for (ResidentApartment aptRes : aptResidents) {
            if (aptRes.getResident().getId().equals(resident.getId()))
                continue;

            // Nếu người đang xem là người thuê/thành viên ở ghép, không coi chủ sở hữu
            // (OWNER) là thành viên gia đình
            if (("TENANT".equals(myRole) || "MEMBER".equals(myRole)) && "OWNER".equals(aptRes.getRole())) {
                continue;
            }

            // Ngược lại, nếu người đang xem là chủ sở hữu, không coi người thuê (TENANT) là
            // thành viên gia đình
            if ("OWNER".equals(myRole) && "TENANT".equals(aptRes.getRole())) {
                continue;
            }

            boolean isAlreadyAdded = familyMembers.stream()
                    .anyMatch(m -> m.residentId().equals(aptRes.getResident().getId()));

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
                .rentalPrice(ra.getRentalPrice())
                .depositAmount(ra.getDepositAmount())
                .build();
    }
}