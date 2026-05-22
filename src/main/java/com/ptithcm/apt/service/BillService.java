package com.ptithcm.apt.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ptithcm.apt.dto.request.CreateBillRequest;
import com.ptithcm.apt.dto.request.UpdateBillStatusRequest;
import com.ptithcm.apt.dto.response.BillSummaryResponse;
import com.ptithcm.apt.dto.response.AdminBillDetailResponse;
import com.ptithcm.apt.dto.response.AdminBillListResponse;
import com.ptithcm.apt.dto.response.UserBillDetailResponse;
import com.ptithcm.apt.dto.response.UserBillListResponse;
import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.dto.response.UpdateBillStatusResponse;
import com.ptithcm.apt.enums.BillStatus;

public interface BillService {
        public BillSummaryResponse createBill(CreateBillRequest req);

        public UpdateBillStatusResponse updateBillStatus(Long billId, UpdateBillStatusRequest req);

        public Page<AdminBillListResponse> getBillsByAdmin(Integer month, Integer year, Long apartmentId,
                        BillStatus status, String roomNumber, Pageable pageable);

        public AdminBillDetailResponse getBillDetailByAdmin(Long id);

        public Page<UserBillListResponse> getMyBills(Integer month, Integer year, Long apartmentId, BillStatus status,
                        Pageable pageable);

        public UserBillDetailResponse getMyBillDetailById(Long id);

        Optional<Bill> findBillEntityById(Long id);

        Optional<Bill> findBillByIdAndUserId(Long billId, Long userId);

        List<Bill> findAllByStatusAndDueDateBefore(BillStatus status, LocalDateTime dateTime);

        boolean isApartmentHasStatus(Long apartmentId, BillStatus status);

}
