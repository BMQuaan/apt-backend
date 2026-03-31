package com.ptithcm.apt.dto.request;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResidentRequest {

    private String fullName;
    private String citizenIdentity; // CCCD

    private String phone;
    private String email;

}