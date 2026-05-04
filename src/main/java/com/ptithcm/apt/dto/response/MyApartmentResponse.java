package com.ptithcm.apt.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class MyApartmentResponse {
    private Long apartmentId;
    private String roomNumber;
    private String role; // OWNER, TENANT, MEMBER
    private Boolean isHead;
    private BigDecimal rentalPrice;
    private LocalDate contractStart;
    private LocalDate contractEnd;
}