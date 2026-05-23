package com.ptithcm.apt.dto.request;

import com.ptithcm.apt.enums.BillStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateBillStatusRequest(
        @NotNull(message = "Trạng thái không được để trống") BillStatus status) {
}
