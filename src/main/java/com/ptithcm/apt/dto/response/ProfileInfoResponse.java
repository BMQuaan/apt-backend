package com.ptithcm.apt.dto.response;

import lombok.Builder;
import java.time.LocalDate;

@Builder
public record ProfileInfoResponse(
        Long residentId,
        String fullName,
        String citizenIdentity,
        String phone,
        String email,
        LocalDate dob
) {}