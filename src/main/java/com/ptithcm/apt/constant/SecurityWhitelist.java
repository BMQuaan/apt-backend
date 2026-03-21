package com.ptithcm.apt.constant;

public final class SecurityWhitelist {
    private SecurityWhitelist() {}

    public static final String[] AUTH_WHITELIST = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh-token",
            "/api/auth/forgot-password",
            "/api/auth/verify-otp",
            "/api/auth/reset-password",
            "/api/auth/test",
            "/api/public/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html"
    };

    public static final String[] PUBLIC_GET_ENDPOINTS = {
    };
}
