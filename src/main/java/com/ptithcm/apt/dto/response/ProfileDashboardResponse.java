package com.ptithcm.apt.dto.response;

import lombok.Builder;
import java.util.List;

@Builder
public record ProfileDashboardResponse(
        ProfileInfoResponse personalInfo,
<<<<<<< HEAD
        List<ProfileApartmentResponse> livingApartments,
=======
        ProfileApartmentResponse livingApartment,
>>>>>>> main
        List<ProfileApartmentResponse> ownedApartments,
        List<FamilyMemberResponse> familyMembers
) {}