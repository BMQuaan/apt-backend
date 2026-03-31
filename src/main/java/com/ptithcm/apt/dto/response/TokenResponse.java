package com.ptithcm.apt.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
        String accessToken,
        String refreshToken,
        UserInfo user
) {
    @Builder
    public record UserInfo(
            Long id,
            String username,
            String role
    ) {}
}