package com.ptithcm.apt.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class ResidentListResponse {
    private Long residentId;
    private String fullName;
    private String citizenIdentity;
    private String phone;

    // Thông tin phòng
    private String roomNumber;
    private String role;
    private Boolean isHead;
    private LocalDate contractStart;
}
