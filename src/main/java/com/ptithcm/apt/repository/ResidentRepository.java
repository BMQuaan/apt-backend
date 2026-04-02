package com.ptithcm.apt.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ptithcm.apt.entity.Resident;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
    boolean existsByCitizenIdentity(String citizenIdentity);

    Optional<Resident> findByCitizenIdentity(String citizenIdentity);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);
}
