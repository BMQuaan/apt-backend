package com.ptithcm.apt.dto.request;

import com.ptithcm.apt.enums.RentStatus;

public record UpdateRentInvoiceStatusRequest(
    RentStatus status
) {

}
