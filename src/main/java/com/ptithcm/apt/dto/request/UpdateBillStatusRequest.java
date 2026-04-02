package com.ptithcm.apt.dto.request;

import com.ptithcm.apt.enums.BillStatus;

public record UpdateBillStatusRequest(
        BillStatus status) {
}
