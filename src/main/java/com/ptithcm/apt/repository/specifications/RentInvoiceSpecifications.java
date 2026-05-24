package com.ptithcm.apt.repository.specifications;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ptithcm.apt.entity.RentInvoice;
import com.ptithcm.apt.enums.RentStatus;

public class RentInvoiceSpecifications {
    
    public static Specification<RentInvoice> hasFilters(Integer month, Integer year, Long apartmentId,
            RentStatus status, String roomNumber) { // Thêm tham số roomNumber ở đây
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

            // Lọc theo số/tên căn hộ (roomNumber) tương tự bên Bill
            if (roomNumber != null && !roomNumber.trim().isEmpty()) {
                // Phép toán tương đương SQL: LOWER(apartment.room_number) LIKE %roomnumber%
                predicates.add(cb.like(
                    cb.lower(root.get("apartment").get("roomNumber")), 
                    "%" + roomNumber.toLowerCase() + "%"
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}