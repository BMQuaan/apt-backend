package com.ptithcm.apt.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ptithcm.apt.dto.request.CreateRentInvoiceRequest;
import com.ptithcm.apt.dto.request.UpdateRentInvoiceStatusRequest;
import com.ptithcm.apt.dto.response.AdminRentInvoiceDetailResponse;
import com.ptithcm.apt.dto.response.AdminRentInvoiceListResponse;
import com.ptithcm.apt.dto.response.RentInvoiceResponse;
import com.ptithcm.apt.dto.response.UpdateRentInvoiceStatusResponse;
import com.ptithcm.apt.dto.response.UserRentInvoiceDetailResponse;
import com.ptithcm.apt.dto.response.UserRentInvoiceListResponse;
import com.ptithcm.apt.entity.RentInvoice;
import com.ptithcm.apt.enums.RentStatus;

public interface RentInvoiceService {
        RentInvoiceResponse createMonthlyRentInvoice(CreateRentInvoiceRequest req);

        Page<AdminRentInvoiceListResponse> getRentInvoiceListByAdmin(Integer month, Integer year, Long apartmentId,
                        RentStatus status, Pageable pageable);

        AdminRentInvoiceDetailResponse getRentInvoiceDetailByAdmin(Long id);

        UpdateRentInvoiceStatusResponse updateRentInvoiceStatus(Long id, UpdateRentInvoiceStatusRequest req);

        Page<UserRentInvoiceListResponse> getMyRentInvoices(Integer month, Integer year, Long apartmentId,
                        RentStatus status, Pageable pageable);

        UserRentInvoiceDetailResponse getMyRentInvoiceDetailById(Long id);

        Optional<RentInvoice> findRentInvoiceEntityById(Long id);

        Optional<RentInvoice> findRentInvoiceByIdAndUserId(Long id, Long userId);

        List<RentInvoice> findAllByStatusAndDueDateBefore(RentStatus status, LocalDateTime dateTime);

        Page<RentInvoice> findMyRentInvoices(Long userId, Long apartmentId, Integer month, Integer year,
                        RentStatus status, Pageable pageable);
}
