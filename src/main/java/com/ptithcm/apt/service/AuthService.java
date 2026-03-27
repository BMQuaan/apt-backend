package com.ptithcm.apt.service;

import com.ptithcm.apt.dto.request.LoginRequest;
import com.ptithcm.apt.dto.request.RefreshTokenRequest;
import com.ptithcm.apt.dto.response.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);
    TokenResponse refreshToken(RefreshTokenRequest request);
}