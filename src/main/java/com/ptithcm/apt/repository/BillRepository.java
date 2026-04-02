package com.ptithcm.apt.repository;

import com.ptithcm.apt.entity.Bill;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByApartmentIdAndBillingMonthAndBillingYear(Long apartmentId, Integer billingMonth,
            Integer billingYear);
}
