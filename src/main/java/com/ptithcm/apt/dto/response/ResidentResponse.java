package com.ptithcm.apt.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ResidentResponse {
    private Long id;
    private String fullName;
    private String citizenIdentity;
    private LocalDate dob;
    private String phone;
    private String email;
}
