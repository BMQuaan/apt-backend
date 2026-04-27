package com.ptithcm.apt.constant;

public final class SecurityWhitelist {
    private SecurityWhitelist() {}

    public static final String[] AUTH_WHITELIST = {
            "/api/v1/auth/login",
            "/api/v1/auth/google-login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/test",
            "/api/public/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html"
    };

    public static final String[] PUBLIC_GET_ENDPOINTS = {
    };
}
