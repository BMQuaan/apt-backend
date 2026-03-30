package com.ptithcm.apt.service;

import java.util.List;

import com.ptithcm.apt.entity.Apartment;

public interface ApartmentService {
    public List<Apartment> getAllApartments();

    Apartment createApartment(Apartment apartment);

    Apartment updateApartment(Integer id, Apartment apartmentDetails);

    Apartment getApartmentById(Integer id);
}
