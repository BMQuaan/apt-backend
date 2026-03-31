package com.ptithcm.apt.controller;

import com.ptithcm.apt.dto.request.LoginRequest;
import com.ptithcm.apt.dto.request.RefreshTokenRequest;
import com.ptithcm.apt.dto.response.ApiResponse;
import com.ptithcm.apt.dto.response.TokenResponse;
import com.ptithcm.apt.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        TokenResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập thành công"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cấp lại token thành công"));
    }
}