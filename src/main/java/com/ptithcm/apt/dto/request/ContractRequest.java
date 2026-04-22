package com.ptithcm.apt.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContractRequest {
    // --- THÔNG TIN NGƯỜI ĐỨNG TÊN (TENANT / OWNER) ---
    @NotBlank(message = "Tên không được để trống")
    private String fullName;

    @NotNull
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate dob;

    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải có 10 chữ số")
    private String phone;

    @NotBlank(message = "CCCD không được để trống")
    @Pattern(regexp = "^\\d{12}$", message = "CCCD phải có đúng 12 chữ số")
    private String citizenIdentity;

    @NotBlank(message = "Email không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@gmail\\.com$", message = "Email phải đúng định dạng")
    private String email;

    // --- THÔNG TIN HỢP ĐỒNG ---
    @NotNull(message = "ID Phòng không được để trống")
    private Long apartmentId;

    @NotBlank(message = "Vai trò không được để trống (TENANT hoặc OWNER)")
    private String role; // Chỉ nhận 'TENANT' hoặc 'OWNER'

    @NotNull(message = "Giá thuê không được để trống")
    @Min(value = 0, message = "Giá thuê không được âm")
    private BigDecimal rentalPrice;

    @NotNull(message = "Tiền cọc không được để trống")
    @Min(value = 0, message = "Tiền cọc không được âm")
    private BigDecimal depositAmount;

    @NotNull(message = "Ngày bắt đầu hợp đồng không được để trống")
    @FutureOrPresent(message = "Ngày bắt đầu hợp đồng không được nằm trong quá khứ")
    private LocalDate contractStart;

    @FutureOrPresent(message = "Ngày kết thúc hợp đồng không được nằm trong quá khứ")
    private LocalDate contractEnd;
}