package com.ptithcm.apt.dto.request;

import com.ptithcm.apt.enums.RentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateRentInvoiceStatusRequest(
    @NotNull(message = "Trạng thái không được để trống") RentStatus status
) {

}
