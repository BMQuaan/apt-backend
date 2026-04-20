package com.ptithcm.apt.service;

import com.ptithcm.apt.dto.request.CreateRentInvoiceRequest;
import com.ptithcm.apt.dto.response.RentInvoiceResponse;

public interface RentInvoiceService {
    RentInvoiceResponse createMonthlyRentInvoice(CreateRentInvoiceRequest req);

}
