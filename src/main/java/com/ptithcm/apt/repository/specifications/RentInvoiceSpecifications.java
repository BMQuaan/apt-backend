package com.ptithcm.apt.repository.specifications;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ptithcm.apt.entity.RentInvoice;
import com.ptithcm.apt.enums.RentStatus;

public class RentInvoiceSpecifications {
    public static Specification<RentInvoice> hasFilters(Integer month, Integer year, Long apartmentId,
            RentStatus status) {
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
