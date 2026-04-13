package com.ptithcm.apt.dto.request;

import com.ptithcm.apt.entity.User;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateRentInvoiceRequest(
                @NotNull Long apartmentId,
                @Min(value = 1, message = "Month must be at least 1") @Max(value = 12, message = "Month must be at most 12") @NotNull Integer month,
                @NotNull @Positive Integer year,
                @NotNull User creator) {

}
