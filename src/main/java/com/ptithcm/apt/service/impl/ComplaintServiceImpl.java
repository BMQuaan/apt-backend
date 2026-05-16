package com.ptithcm.apt.service.impl;

import com.ptithcm.apt.dto.request.CreateComplaintRequest;
import com.ptithcm.apt.dto.request.UpdateComplaintStatusRequest;
import com.ptithcm.apt.dto.response.ComplaintResponse;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.Complaint;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.repository.ApartmentRepository;
import com.ptithcm.apt.repository.ComplaintRepository;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.repository.ResidentRepository;
import com.ptithcm.apt.repository.UserRepository;
import com.ptithcm.apt.service.ComplaintService;
import com.ptithcm.apt.utils.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    private static final String STATUS_RECEIVED = "RECEIVED";
    private static final String STATUS_DONE = "DONE";

    private final ComplaintRepository complaintRepository;
    private final ApartmentRepository apartmentRepository;
    private final ResidentRepository residentRepository;
    private final ResidentApartmentRepository residentApartmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ComplaintResponse createComplaint(CreateComplaintRequest request) {
        Resident resident = getCurrentResident();
        Apartment apartment = resolveApartmentForResident(resident, request.apartmentId());

        Complaint complaint = Complaint.builder()
                .resident(resident)
                .apartment(apartment)
                .category(normalizeCategory(request.category()))
                .title(request.title())
                .content(request.content())
                .status(STATUS_RECEIVED)
                .build();

        return toResponse(complaintRepository.save(complaint));
    }

    @Override
    public List<ComplaintResponse> getMyComplaints() {
        Resident resident = getCurrentResident();
        return complaintRepository.findByResident_IdOrderByCreatedAtDesc(resident.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ComplaintResponse> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ComplaintResponse updateStatus(Long id, UpdateComplaintStatusRequest request) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khiếu nại"));

        String status = normalizeStatus(request.status());
        complaint.setStatus(status);

        if (STATUS_DONE.equals(status)) {
            User currentUser = userRepository.findByUsername(SecurityUtils.getCurrentUsername())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));
            complaint.setResolvedBy(currentUser);
            complaint.setResolvedAt(LocalDateTime.now());
        } else {
            complaint.setResolvedBy(null);
            complaint.setResolvedAt(null);
        }

        return toResponse(complaintRepository.save(complaint));
    }

    private Resident getCurrentResident() {
        User currentUser = userRepository.findByUsername(SecurityUtils.getCurrentUsername())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));
        return residentRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ cư dân cho tài khoản hiện tại"));
    }

    private Apartment resolveApartmentForResident(Resident resident, Long requestedApartmentId) {
        List<ResidentApartment> activeApartments =
                residentApartmentRepository.findByResident_IdAndIsActiveTrue(resident.getId());

        if (activeApartments.isEmpty()) {
            throw new IllegalArgumentException("Cư dân hiện tại chưa có căn hộ đang hoạt động");
        }

        if (requestedApartmentId == null) {
            return activeApartments.get(0).getApartment();
        }

        boolean belongsToResident = activeApartments.stream()
                .anyMatch(ra -> ra.getApartment().getId().equals(requestedApartmentId));
        if (!belongsToResident) {
            throw new IllegalArgumentException("Bạn không có quyền gửi khiếu nại cho căn hộ này");
        }

        return apartmentRepository.findById(requestedApartmentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy căn hộ"));
    }

    private String normalizeCategory(String category) {
        String normalized = category.trim().toUpperCase();
        if (!List.of("REPAIR", "NOISE", "SERVICE", "OTHER").contains(normalized)) {
            throw new IllegalArgumentException("category chỉ được là REPAIR, NOISE, SERVICE hoặc OTHER");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!STATUS_RECEIVED.equals(normalized) && !STATUS_DONE.equals(normalized)) {
            throw new IllegalArgumentException("status chỉ được là RECEIVED hoặc DONE");
        }
        return normalized;
    }

    private ComplaintResponse toResponse(Complaint complaint) {
        Apartment apartment = complaint.getApartment();
        Resident resident = complaint.getResident();

        return new ComplaintResponse(
                complaint.getId(),
                complaint.getCategory(),
                complaint.getTitle(),
                complaint.getContent(),
                complaint.getStatus(),
                apartment != null ? apartment.getId() : null,
                apartment != null ? apartment.getRoomNumber() : null,
                resident != null ? resident.getId() : null,
                resident != null ? resident.getFullName() : null,
                complaint.getCreatedAt(),
                complaint.getResolvedAt()
        );
    }
}
