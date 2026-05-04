package com.ptithcm.apt.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentDetailResponse {
    private Long id;
    private String fullName;
    private String citizenIdentity;
    private String dob;
    private String phone;
    private String email;

    private List<ResidencyInfo> residencies;

    @Data
    @Builder
    public static class ResidencyInfo {
        private Long apartmentId;
        private String roomNumber;
        private String role;
        private Boolean isHead;
    }
}
