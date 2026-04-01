package com.ptithcm.apt.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ptithcm.apt.dto.request.BillRequest;
import com.ptithcm.apt.dto.request.UpdateBillStatusRequest;
import com.ptithcm.apt.dto.response.CreateBillResponse;
import com.ptithcm.apt.dto.response.UpdateBillStatusResponse;
import com.ptithcm.apt.entity.Bill;

public interface BillService {
    public CreateBillResponse createBill(BillRequest req);
    public UpdateBillStatusResponse updateBillStatus(Long billId, UpdateBillStatusRequest req);
    public Page<Bill> getBillsByAdmin(Pageable pageable);

}
