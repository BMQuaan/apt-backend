package com.ptithcm.apt.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class ContractResponse {
    private Long id; // ID của bảng resident_apartments
    private Long residentId;
    private Long apartmentId;
    private String roomNumber;
    private String residentName;
    private String citizenIdentity;
    private String phone;
    private String role; // TENANT, OWNER, MEMBER
    private Boolean isHead;
    private BigDecimal rentalPrice;
    private BigDecimal depositAmount;
    private LocalDate contractStart;
    private LocalDate contractEnd;
    private Boolean isActive;
}