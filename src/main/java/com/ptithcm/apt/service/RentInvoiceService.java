package com.ptithcm.apt.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ptithcm.apt.dto.request.CreateRentInvoiceRequest;
import com.ptithcm.apt.dto.request.UpdateRentInvoiceStatusRequest;
import com.ptithcm.apt.dto.response.AdminRentInvoiceDetailResponse;
import com.ptithcm.apt.dto.response.AdminRentInvoiceListResponse;
import com.ptithcm.apt.dto.response.RentInvoiceResponse;
import com.ptithcm.apt.dto.response.UpdateRentInvoiceStatusReponse;
import com.ptithcm.apt.enums.RentStatus;

public interface RentInvoiceService {
    RentInvoiceResponse createMonthlyRentInvoice(CreateRentInvoiceRequest req);

    Page<AdminRentInvoiceListResponse> getRentInvoiceListByAdmin(Integer month, Integer year, Long apartmentId,
                        RentStatus status, Pageable pageable);

    AdminRentInvoiceDetailResponse getRentInvoiceDetailByAdmin(Long id);

    UpdateRentInvoiceStatusReponse updateRentInvoiceStatus(Long id, UpdateRentInvoiceStatusRequest req);
}
