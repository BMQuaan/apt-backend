package com.ptithcm.apt.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ptithcm.apt.dto.request.ApartmentRequest;
import com.ptithcm.apt.dto.response.ApartmentResponse;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.repository.ApartmentRepository;
import com.ptithcm.apt.service.ApartmentService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Service
@RequiredArgsConstructor
public class ApartmentServiceImpl implements ApartmentService {

    private final ApartmentRepository apartmentRepository;

    @Override
    public Page<ApartmentResponse> getAllApartments(int page) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<Apartment> apartmentPage = apartmentRepository.findAll(pageable);
        return apartmentPage.map(entity -> new ApartmentResponse(
                entity.getId(),
                entity.getRoomNumber(),
                entity.getFloor(),
                entity.getArea(),
                entity.getStatus(),
                entity.getCreatedAt()));
    }

    @Override
    public ApartmentResponse createApartment(ApartmentRequest apartment) {
        if (apartment.getRoomNumber() == null || apartment.getRoomNumber().trim().isEmpty()) {
            throw new RuntimeException("The room number must not be left blank");
        }
        if (apartment.getFloor() == null || apartment.getFloor() <= 0) {
            throw new RuntimeException("The floor number must be greater than 0");
        }
        if (apartment.getArea() == null || apartment.getArea().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("The room area must be greater than 0");
        }

        String expectedFormat = "^" + apartment.getFloor() + "0\\d+$";
        if (!apartment.getRoomNumber().matches(expectedFormat)) {
            throw new RuntimeException("Wrong room number format! For floor "
                    + apartment.getFloor() + ", the room number must be in the format "
                    + apartment.getFloor() + "0x (Example: " + apartment.getFloor() + "01)");
        }

        if (apartmentRepository.existsByRoomNumber(apartment.getRoomNumber())) {
            throw new RuntimeException("The room number already exists!");
        }

        // Tạo Entity để chuẩn bị lưu vào DB
        Apartment apartmentSave = new Apartment();
        apartmentSave.setRoomNumber(apartment.getRoomNumber());
        apartmentSave.setFloor(apartment.getFloor());
        apartmentSave.setArea(apartment.getArea());

        if (apartment.getStatus() == null || apartment.getStatus().trim().isEmpty()) {
            apartmentSave.setStatus("AVAILABLE");
        } else if (!apartment.getStatus().equals("AVAILABLE") &&
                !apartment.getStatus().equals("RENTED") &&
                !apartment.getStatus().equals("OWNED")) {
            throw new RuntimeException("Invalid status! Allowed values are: AVAILABLE, RENTED, OWNED");
        } else {
            apartmentSave.setStatus(apartment.getStatus());
        }

        Apartment savedEntity = apartmentRepository.save(apartmentSave);

        return new ApartmentResponse(
                savedEntity.getId(),
                savedEntity.getRoomNumber(),
                savedEntity.getFloor(),
                savedEntity.getArea(),
                savedEntity.getStatus(),
                savedEntity.getCreatedAt());
    }

    @Override
    public ApartmentResponse updateApartment(Long id, ApartmentRequest apartmentDetails) { // Interface
        Apartment apartment = apartmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not Found Room ID: " + id));

        if (apartmentDetails.getRoomNumber() == null || apartmentDetails.getRoomNumber().trim().isEmpty()) {
            throw new RuntimeException("The room number must not be left blank");
        }
        if (apartmentDetails.getFloor() == null || apartmentDetails.getFloor() <= 0) {
            throw new RuntimeException("The floor number must be greater than 0");
        }
        if (apartmentDetails.getArea() == null || apartmentDetails.getArea().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("The room area must be greater than 0");
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

        Apartment updatedEntity = apartmentRepository.save(apartment);

        return new ApartmentResponse(
                updatedEntity.getId(),
                updatedEntity.getRoomNumber(),
                updatedEntity.getFloor(),
                updatedEntity.getArea(),
                updatedEntity.getStatus(),
                updatedEntity.getCreatedAt());
    }

    @Override
    public List<ApartmentResponse> searchApartmentsByRoomNumber(String keyword) {
        List<Apartment> apartments = apartmentRepository.findByRoomNumberContaining(keyword);

        return apartments.stream()
                .map(entity -> new ApartmentResponse(
                        entity.getId(),
                        entity.getRoomNumber(),
                        entity.getFloor(),
                        entity.getArea(),
                        entity.getStatus(),
                        entity.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public ApartmentResponse getApartmentById(Long id) {
        Apartment apartment = apartmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not Found Room ID: " + id));

        return new ApartmentResponse(
                apartment.getId(),
                apartment.getRoomNumber(),
                apartment.getFloor(),
                apartment.getArea(),
                apartment.getStatus(),
                apartment.getCreatedAt());
    }

    @Override
    public List<ApartmentResponse> getApartmentsByStatus(String status) {
        List<Apartment> apartments = apartmentRepository.findByStatus(status);
        return apartments.stream()
                .map(entity -> new ApartmentResponse(
                        entity.getId(), entity.getRoomNumber(), entity.getFloor(),
                        entity.getArea(), entity.getStatus(), entity.getCreatedAt()))
                .collect(Collectors.toList());
    }
}