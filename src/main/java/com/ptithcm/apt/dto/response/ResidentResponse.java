package com.ptithcm.apt.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResidentResponse {
    private Long id;
    private String fullName;
    private String citizenIdentity;
    private String phone;
    private String email;
    private LocalDate dob;

    private Integer userId;
    private LocalDateTime createdAt;
}
