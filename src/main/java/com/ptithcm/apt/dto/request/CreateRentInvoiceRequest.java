package com.ptithcm.apt.dto.request;

import com.ptithcm.apt.entity.User;

public record CreateRentInvoiceRequest(
        Long apartmentId,
        Integer month,
        Integer year,
        User creator) {

}
