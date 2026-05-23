package com.ptithcm.apt.dto.request;

import java.math.BigDecimal;

import com.ptithcm.apt.entity.Apartment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateMonthlyMetricRequest(
                @NotNull(message = "Căn hộ không được để trống") Apartment apartment,
                @Min(value = 1, message = "Tháng phải từ 1 đến 12") @Max(value = 12, message = "Tháng phải từ 1 đến 12") @NotNull(message = "Tháng không được để trống") Integer month,
                @NotNull(message = "Năm không được để trống") @Positive(message = "Năm phải là số dương") Integer year,
                @NotNull(message = "Chỉ số điện mới không được để trống") @Positive(message = "Chỉ số điện mới phải là số dương") BigDecimal electricityNew,
                @NotNull(message = "Chỉ số nước mới không được để trống") @Positive(message = "Chỉ số nước mới phải là số dương") BigDecimal waterNew,
                @NotNull(message = "Chỉ số điện cũ không được để trống") @Positive(message = "Chỉ số điện cũ phải là số dương") BigDecimal electricityOld,
                @NotNull(message = "Chỉ số nước cũ không được để trống") @Positive(message = "Chỉ số nước cũ phải là số dương") BigDecimal waterOld) {

}
