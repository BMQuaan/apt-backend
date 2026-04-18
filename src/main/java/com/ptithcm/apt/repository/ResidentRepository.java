package com.ptithcm.apt.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ptithcm.apt.entity.Resident;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
    boolean existsByCitizenIdentity(String citizenIdentity);

    Optional<Resident> findByCitizenIdentity(String citizenIdentity);

    Optional<Resident> findByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    Optional<Resident> findByUser_Id(Long userId);
}
