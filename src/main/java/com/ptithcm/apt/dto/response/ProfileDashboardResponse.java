package com.ptithcm.apt.dto.response;

import lombok.Builder;
import java.util.List;

@Builder
public record ProfileDashboardResponse(
                ProfileInfoResponse personalInfo,
                ProfileApartmentResponse livingApartment,
                List<ProfileApartmentResponse> ownedApartments,
                List<FamilyMemberResponse> familyMembers) {
}