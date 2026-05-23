package com.ptithcm.apt.dto.request;

import com.ptithcm.apt.entity.User;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateRentInvoiceRequest(
                @NotNull(message = "ID căn hộ không được để trống") Long apartmentId,
                @Min(value = 1, message = "Tháng phải từ 1 đến 12") @Max(value = 12, message = "Tháng phải từ 1 đến 12") @NotNull(message = "Tháng không được để trống") Integer month,
                @NotNull(message = "Năm không được để trống") @Positive(message = "Năm phải là số dương") Integer year,
                @NotNull(message = "Người tạo không được để trống") User creator) {

}
