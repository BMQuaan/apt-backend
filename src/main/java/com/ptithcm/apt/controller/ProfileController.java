package com.ptithcm.apt.controller;

import com.ptithcm.apt.dto.response.ApiResponse;
import com.ptithcm.apt.dto.response.ProfileDashboardResponse;
import com.ptithcm.apt.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    public final ProfileService profileService;

<<<<<<< HEAD
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ProfileDashboardResponse>> getProfileDashboard() {
        return ResponseEntity.ok(ApiResponse.success(profileService.getProfileDashboard(), "Lấy tổng quan hồ sơ thành công"));
=======
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileDashboardResponse>> getProfileDashboard() {
        return ResponseEntity
                .ok(ApiResponse.success(profileService.getProfileDashboard(), "Lấy tổng quan hồ sơ thành công"));
>>>>>>> main
    }
}
