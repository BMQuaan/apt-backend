package com.ptithcm.apt.service;

import com.ptithcm.apt.dto.request.CreateRentInvoiceRequest;
import com.ptithcm.apt.dto.response.CreateRentInvoiceResponse;

public interface RentInvoiceService {
    CreateRentInvoiceResponse createMonthlyRentInvoice(CreateRentInvoiceRequest req);

}
