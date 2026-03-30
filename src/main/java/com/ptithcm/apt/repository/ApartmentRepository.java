package com.ptithcm.apt.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ptithcm.apt.entity.Apartment;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {
    
}
    

