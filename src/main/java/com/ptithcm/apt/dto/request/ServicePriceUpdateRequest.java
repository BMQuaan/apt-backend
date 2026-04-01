package com.ptithcm.apt.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ServicePriceUpdateRequest(
        @NotEmpty(message = "Danh sách dịch vụ không được để trống")
        @Valid
        List<ServicePrice> prices
) {
    public record ServicePrice(
            @NotBlank(message = "Mã dịch vụ không được để trống")
            String serviceCode, // ELECTRICITY, WATER, MANAGEMENT, SANITATION

            @NotNull(message = "Giá mới không được để trống")
            @PositiveOrZero(message = "Giá dịch vụ phải lớn hơn hoặc bằng 0")
            BigDecimal newPrice,

            @NotNull(message = "Ngày áp dụng không được để trống")
            LocalDate effectiveFrom
    ) {}
}