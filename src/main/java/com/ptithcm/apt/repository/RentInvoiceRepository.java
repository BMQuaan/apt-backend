package com.ptithcm.apt.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ptithcm.apt.entity.RentInvoice;
import com.ptithcm.apt.enums.RentStatus;

@Repository
public interface RentInvoiceRepository extends JpaRepository<RentInvoice, Long>, JpaSpecificationExecutor<RentInvoice> {
        @Query("SELECT ri FROM RentInvoice ri " +
                        "JOIN ri.apartment a " +
                        "JOIN ResidentApartment ra ON ra.apartment.id = a.id " +
                        "JOIN Resident r ON r.id = ra.resident.id " +
                        "WHERE r.user.id = :userId " +
                        "AND ra.isActive = true " +
                        // Chỉ lấy những hóa đơn mà người dùng này là người thuê (TENANT) hoặc chủ nhà
                        // (OWNER)
                        "AND (ra.role = 'TENANT' OR ra.role = 'OWNER') " +
                        "AND (cast(:apartmentId as text) IS NULL OR a.id = :apartmentId) " +
                        "AND (cast(:month as integer) IS NULL OR ri.billingMonth = :month) " +
                        "AND (cast(:year as integer) IS NULL OR ri.billingYear = :year) " +
                        "AND (cast(:status as text) IS NULL OR ri.status = :status)")
        Page<RentInvoice> findMyRentInvoices(@Param("userId") Long userId,
                        @Param("apartmentId") Long apartmentId,
                        @Param("month") Integer month,
                        @Param("year") Integer year,
                        @Param("status") RentStatus status,
                        Pageable pageable);

        @Query("SELECT ri FROM RentInvoice ri " +
                        "JOIN ri.apartment a " +
                        "JOIN ResidentApartment ra ON ra.apartment.id = a.id " +
                        "JOIN Resident r ON r.id = ra.resident.id " +
                        "WHERE ri.id = :id AND r.user.id = :userId " +
                        "AND ra.isActive = true " +
                        "AND (ra.role = 'TENANT' OR ra.role = 'OWNER')")
        Optional<RentInvoice> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
