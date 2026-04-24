package com.ptithcm.apt.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "ID Token từ Google không được để trống")
        String idToken
) {}