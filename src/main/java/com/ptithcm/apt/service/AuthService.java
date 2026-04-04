package com.ptithcm.apt.service;

import com.ptithcm.apt.dto.request.*;
import com.ptithcm.apt.dto.response.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    TokenResponse login(LoginRequest request, HttpServletRequest httpRequest);
    TokenResponse refreshToken(RefreshTokenRequest request);

    void forgotPassword(ForgotPasswordRequest request);
    String verifyOtp(VerifyOtpRequest request);
    void resetPassword(ResetPasswordRequest request);
    void changePassword(ChangePasswordRequest request);
}