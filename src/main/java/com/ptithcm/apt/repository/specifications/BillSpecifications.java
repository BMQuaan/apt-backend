package com.ptithcm.apt.repository.specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ptithcm.apt.entity.Bill;
import com.ptithcm.apt.enums.BillStatus;

import jakarta.persistence.criteria.Predicate;

public class BillSpecifications {
    public static Specification<Bill> hasFilters(Integer month, Integer year, Long apartmentId, BillStatus status) {
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
