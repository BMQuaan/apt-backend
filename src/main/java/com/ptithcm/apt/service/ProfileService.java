package com.ptithcm.apt.service;

import com.ptithcm.apt.dto.response.FamilyMemberResponse;
import com.ptithcm.apt.dto.response.ProfileApartmentResponse;
import com.ptithcm.apt.dto.response.ProfileDashboardResponse;
import com.ptithcm.apt.dto.response.ProfileInfoResponse;

import java.util.List;

public interface ProfileService {

    // 1. API gộp dashboard
    ProfileDashboardResponse getProfileDashboard();

    // 2. Các API lẻ
    ProfileInfoResponse getMyProfile();

    List<ProfileApartmentResponse> getLivingApartments();

    List<ProfileApartmentResponse> getOwnedApartments();

    List<FamilyMemberResponse> getFamilyMembers();
}