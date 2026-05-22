package com.ptithcm.apt.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ptithcm.apt.dto.response.AdminBillDetailResponse;
import com.ptithcm.apt.dto.response.AdminBillListResponse;
import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.enums.BillStatus;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.mapper.BillMapper;
import com.ptithcm.apt.repository.BillRepository;
import com.ptithcm.apt.repository.specifications.BillSpecifications;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminBillQueryService {

        private final BillRepository billRepository;
        private final BillMapper billMapper;

        /**
         * Lấy danh sách bill cho Admin với filter.
         */
        public Page<AdminBillListResponse> getBillsByAdmin(Integer month, Integer year, Long apartmentId,
                        BillStatus status, String roomNumber, Pageable pageable) {
                Specification<Bill> spec = BillSpecifications.hasFilters(month, year, apartmentId, status, roomNumber);
                Page<Bill> bills = billRepository.findAll(spec, pageable);
                return bills.map(billMapper::toGetBillsByAdminResponse);
        }

        /**
         * Lấy chi tiết bill cho Admin.
         */
        public AdminBillDetailResponse getBillDetailByAdmin(Long id) {
                Bill bill = billRepository.findById(id).orElseThrow(() -> new NotFoundException("Bill not found"));
                return billMapper.toGetBillDetailByAdminResponse(bill);
        }
}
