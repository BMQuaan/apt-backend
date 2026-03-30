package com.ptithcm.apt.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.repository.ApartmentRepository;
import com.ptithcm.apt.service.ApartmentService;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Service
public class ApartmentServiceImpl implements ApartmentService {
    @Autowired
    private ApartmentRepository apartmentRepository;

    @Override
    public List<Apartment> getAllApartments() {
        return apartmentRepository.findAll();
    }

    @Override
    public Apartment createApartment(Apartment apartment) {
        if (apartment.getRoomNumber() == null
                || apartment.getRoomNumber().trim().isEmpty()) {
            throw new RuntimeException("The room number must not be left blank");
        }
        if (apartment.getFloor() == null || apartment.getFloor() <= 0) {
            throw new RuntimeException("The floor number must be greater than 0 and do not leave empty");
        }
        if (apartment.getArea() == null || apartment.getArea().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("The room area must be greater than 0 and do not leave empty");
        }

        String expectedFormart = "^" + apartment.getFloor() + "0\\d+$";
        if (!apartment.getRoomNumber().matches(expectedFormart)) {
            throw new RuntimeException("Wrong room number format! For floor "
                    + apartment.getFloor() + ", the room number must be in the format "
                    + apartment.getFloor() + "0x (Example: " + apartment.getFloor() + "01)");
        }

        if (apartmentRepository.existsByRoomNumber(apartment.getRoomNumber())) {
            throw new RuntimeException("The room number already exists!");
        }

        if (apartment.getStatus() == null || apartment.getStatus().trim().isEmpty()) {
            apartment.setStatus("AVAILABLE");
        } else if (!apartment.getStatus().equals("AVAILABLE") &&
                !apartment.getStatus().equals("RENTED") &&
                !apartment.getStatus().equals("OWNED")) {
            throw new RuntimeException("Invalid status! Allowed values are: AVAILABLE, RENTED, OWNED");
        }
        return apartmentRepository.save(apartment);
    }

    @Override
    public Apartment updateApartment(Integer id, Apartment apartmentDetails) {
        Apartment apartment = apartmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not Found Room ID: " + id));

        if (apartmentDetails.getRoomNumber() == null
                || apartmentDetails.getRoomNumber().trim().isEmpty()) {
            throw new RuntimeException("The room number must not be left blank");
        }
        if (apartmentDetails.getFloor() == null || apartmentDetails.getFloor() <= 0) {
            throw new RuntimeException("The floor number must be greater than 0 do not leave empty");
        }
        if (apartmentDetails.getArea() == null || apartmentDetails.getArea().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("The room area must be greater than 0 do not leave empty");
        }

        String expectedFormat = "^" + apartmentDetails.getFloor() + "0\\d+$";
        if (!apartmentDetails.getRoomNumber().matches(expectedFormat)) {
            throw new RuntimeException("Wrong room number format! For floor "
                    + apartmentDetails.getFloor() + ", the room number must be in the format "
                    + apartmentDetails.getFloor() + "0x (Example: " + apartmentDetails.getFloor() + "01)");
        }

        if (!apartment.getRoomNumber().equals(apartmentDetails.getRoomNumber()) &&
                apartmentRepository.existsByRoomNumber(apartmentDetails.getRoomNumber())) {
            throw new RuntimeException("This new room number has already been used!");
        }

        apartment.setRoomNumber(apartmentDetails.getRoomNumber());
        apartment.setFloor(apartmentDetails.getFloor());
        apartment.setArea(apartmentDetails.getArea());
        if (apartmentDetails.getStatus() != null && !apartmentDetails.getStatus().trim().isEmpty()) {
            if (!apartmentDetails.getStatus().equals("AVAILABLE") &&
                    !apartmentDetails.getStatus().equals("RENTED") &&
                    !apartmentDetails.getStatus().equals("OWNED")) {
                throw new RuntimeException("Invalid status! Allowed values are: AVAILABLE, RENTED, OWNED");
            }
            apartment.setStatus(apartmentDetails.getStatus());
        }

        return apartmentRepository.save(apartment);
    }

    public Apartment getApartmentById(Integer id) {
        return apartmentRepository.findById(id).orElseThrow(() -> new RuntimeException("Not Found Room ID: " + id));
    }
}
