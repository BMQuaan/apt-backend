package com.ptithcm.apt.service;

import java.util.List;

import com.ptithcm.apt.dto.request.ApartmentRequest;
import com.ptithcm.apt.dto.response.ApartmentResponse;
import com.ptithcm.apt.entity.Apartment;

public interface ApartmentService {
    List<ApartmentResponse> getAllApartments();

    ApartmentResponse createApartment(ApartmentRequest apartmentRequest);

    ApartmentResponse updateApartment(Integer id, ApartmentRequest apartmentRequestDetails);

    ApartmentResponse getApartmentById(Integer id);
}
