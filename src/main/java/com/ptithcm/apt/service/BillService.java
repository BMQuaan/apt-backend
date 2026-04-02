package com.ptithcm.apt.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ptithcm.apt.dto.request.BillRequest;
import com.ptithcm.apt.dto.request.UpdateBillStatusRequest;
import com.ptithcm.apt.dto.response.CreateBillResponse;
import com.ptithcm.apt.dto.response.GetBillsByAdminResponse;
import com.ptithcm.apt.dto.response.UpdateBillStatusResponse;
import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.enums.BillStatus;

public interface BillService {
    public CreateBillResponse createBill(BillRequest req);

    public UpdateBillStatusResponse updateBillStatus(Long billId, UpdateBillStatusRequest req);

    public Page<GetBillsByAdminResponse> getBillsByAdmin(Integer month, Integer year, Long apartmentId,
            BillStatus status, Pageable pageable);

}
