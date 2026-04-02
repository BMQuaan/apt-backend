package com.ptithcm.apt.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class UpdateResidentRequest {
    @NotBlank(message = "Họ và tên không được để trống!")
    private String fullName;
    private String phone;
    private String email;
    private LocalDate dob;
}
