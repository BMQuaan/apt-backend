package com.ptithcm.apt.repository;

import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.enums.BillStatus;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long>, JpaSpecificationExecutor<Bill> {

        @Query("SELECT b FROM Bill b " +
                        "JOIN b.apartment a " +
                        "JOIN ResidentApartment ra ON ra.apartment.id = a.id " +
                        "JOIN Resident r ON r.id = ra.resident.id " +
                        "WHERE r.user.id = :userId " +
                        // Ép kiểu cho các tham số kiểm tra NULL
                        "AND (cast(:apartmentId as text) IS NULL OR a.id = :apartmentId) " +
                        "AND (cast(:month as integer) IS NULL OR b.billingMonth = :month) " +
                        "AND (cast(:year as integer) IS NULL OR b.billingYear = :year) " +
                        "AND (cast(:status as text) IS NULL OR b.status = :status) " +
                        "AND ra.isActive = true")
        Page<Bill> findMyBills(@Param("userId") Long userId,
                        @Param("apartmentId") Long apartmentId,
                        @Param("month") Integer month,
                        @Param("year") Integer year,
                        @Param("status") BillStatus status,
                        Pageable pageable);

        @Query("SELECT b FROM Bill b " +
                        "JOIN b.apartment a " +
                        "JOIN ResidentApartment ra ON ra.apartment.id = a.id " +
                        "JOIN Resident r ON r.id = ra.resident.id " +
                        "WHERE b.id = :billId AND r.user.id = :userId AND ra.isActive = true")
        Optional<Bill> findByIdAndUserId(@Param("billId") Long billId, @Param("userId") Long userId);

        boolean existsByApartmentIdAndStatus(Long apartmentId, BillStatus status);
}
