package com.ptithcm.apt.dto.response;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentDetailResponse {
    // Thông tin cá nhân (Bảng Resident)
    private Long id;
    private String fullName;
    private String citizenIdentity;
    private LocalDate dob;
    private String phone;
    private String email;

    // Thông tin cư trú (Bảng ResidentApartment)
    private Long apartmentId;
    private String roomNumber;
    private String role;
    private Boolean isHead;
}
