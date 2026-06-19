package com.ptithcm.apt.repository;

import com.ptithcm.apt.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByResident_IdOrderByCreatedAtDesc(Long residentId);

    List<Complaint> findAllByOrderByCreatedAtDesc();
}
