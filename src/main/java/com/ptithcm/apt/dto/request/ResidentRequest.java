package com.ptithcm.apt.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ResidentRequest {
    // 1. Thông tin cá nhân
    private String fullName;
    private String citizenIdentity; // Căn cước công dân
    private String phone;
    private String email;
    private LocalDate dob; // Ngày sinh

    // 2. Thông tin nơi ở (Ghi vào bảng ResidentApartment)
    private Long apartmentId;
    private BigDecimal rentalPrice;
    private BigDecimal depositAmount;
    private String role; // "OWNER" (Chủ nhà), "TENANT" (Người thuê), "MEMBER" (Thành viên)
    private Boolean isHead; // Có phải chủ hộ không?
    private LocalDate contractStart;
    private LocalDate contractEnd; // Có thể null nếu vô thời hạn
}