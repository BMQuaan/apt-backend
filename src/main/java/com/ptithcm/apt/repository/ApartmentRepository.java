package com.ptithcm.apt.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ptithcm.apt.entity.Apartment;
import java.util.List;
import java.util.Optional;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {
    boolean existsByRoomNumber(String roomNumber);

    List<Apartment> findByStatus(String status);

    List<Apartment> findByRoomNumberContaining(String roomNumber);

    Optional<Apartment> findByRoomNumber(String roomNumber);

}
