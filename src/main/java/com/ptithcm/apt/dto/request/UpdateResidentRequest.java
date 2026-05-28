package com.ptithcm.apt.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
public class UpdateResidentRequest {

    @NotBlank(message = "Họ và tên không được để trống!")
    private String fullName;

    @NotBlank(message = "CCCD không được để trống")
    @Pattern(regexp = "^\\d{12}$", message = "CCCD phải có đúng 12 chữ số")
    private String citizenIdentity;

    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải có 10 chữ số")
    private String phone;

    @NotBlank(message = "Email không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@gmail\\.com$", message = "Email phải đúng định dạng")
    private String email;

    private LocalDate dob;
}