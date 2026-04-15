package com.ptithcm.apt.dto.response;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record ProfileApartmentResponse(
        Long apartmentId,
        String roomNumber,
        Integer floor,
        BigDecimal area,
        String role, // OWNER, TENANT, MEMBER
        Boolean isHead,
        LocalDate contractStart,
        LocalDate contractEnd,
        BigDecimal rentalPrice,
        BigDecimal depositAmount
) {}