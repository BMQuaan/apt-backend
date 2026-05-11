package com.ptithcm.apt.repository.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.enums.BillStatus;

import jakarta.persistence.criteria.Predicate;

public class BillSpecifications {

    public static Specification<Bill> hasFilters(Integer month, Integer year, Long apartmentId, BillStatus status, String roomNumber) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (month != null) {
                predicates.add(cb.equal(root.get("billingMonth"), month));
            }
            if (year != null) {
                predicates.add(cb.equal(root.get("billingYear"), year));
            }
            if (apartmentId != null) {
                predicates.add(cb.equal(root.get("apartment").get("id"), apartmentId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (roomNumber != null && !roomNumber.trim().isEmpty()) {
                // Phép toán: apartment.roomNumber LIKE %roomNumber%
                // cb.lower giúp tìm kiếm không phân biệt hoa thường (case-insensitive)
                predicates.add(cb.like(
                    cb.lower(root.get("apartment").get("roomNumber")), 
                    "%" + roomNumber.toLowerCase() + "%"
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}