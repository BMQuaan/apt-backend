package com.ptithcm.apt.dto.response;

import lombok.Builder;
import java.time.LocalDate;

@Builder
public record FamilyMemberResponse(
        Long residentId,
        String fullName,
        String phone,
        LocalDate dob,
        String role,
        Boolean isHead
) {}