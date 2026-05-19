package com.ptithcm.apt.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import com.ptithcm.apt.dto.request.ApartmentRequest;
import com.ptithcm.apt.dto.response.ApartmentResponse;
import com.ptithcm.apt.entity.Apartment;

public interface ApartmentService {
    Page<ApartmentResponse> getAllApartments(int page);

    ApartmentResponse createApartment(ApartmentRequest apartmentRequest);

    ApartmentResponse updateApartment(Long id, ApartmentRequest apartmentRequestDetails);

    List<ApartmentResponse> searchApartmentsByRoomNumber(String id);

    ApartmentResponse getApartmentById(Long id);

    List<ApartmentResponse> getApartmentsByStatus(String status);

    Optional<Apartment> findById(Long id);

    boolean existsById(Long id);
}
