package com.ptithcm.apt.service;

import com.ptithcm.apt.dto.request.LoginRequest;
import com.ptithcm.apt.dto.request.RefreshTokenRequest;
import com.ptithcm.apt.dto.response.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    TokenResponse login(LoginRequest request, HttpServletRequest httpRequest);
    TokenResponse refreshToken(RefreshTokenRequest request);
}