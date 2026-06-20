package com.ptithcm.apt.service.impl;

import com.ptithcm.apt.dto.request.CreateNotificationRequest;
import com.ptithcm.apt.dto.response.NotificationResponse;
import com.ptithcm.apt.dto.response.NotificationTargetResponse;
import com.ptithcm.apt.entity.Apartment;
import com.ptithcm.apt.entity.Notification;
import com.ptithcm.apt.entity.NotificationRecipient;
import com.ptithcm.apt.entity.Resident;
import com.ptithcm.apt.entity.ResidentApartment;
import com.ptithcm.apt.entity.User;
import com.ptithcm.apt.exception.NotFoundException;
import com.ptithcm.apt.repository.ApartmentRepository;
import com.ptithcm.apt.repository.NotificationRecipientRepository;
import com.ptithcm.apt.repository.NotificationRepository;
import com.ptithcm.apt.repository.ResidentApartmentRepository;
import com.ptithcm.apt.repository.ResidentRepository;
import com.ptithcm.apt.repository.UserRepository;
import com.ptithcm.apt.service.NotificationService;
import com.ptithcm.apt.utils.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String TARGET_ALL = "ALL";
    private static final String TARGET_SPECIFIC = "SPECIFIC";

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final ApartmentRepository apartmentRepository;
    private final ResidentRepository residentRepository;
    private final ResidentApartmentRepository residentApartmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));

        String targetType = normalizeTargetType(request.targetType());
        List<Apartment> targetApartments = resolveTargetApartments(targetType, request.apartmentIds());

        Notification notification = Notification.builder()
                .title(request.title())
                .content(request.content())
                .targetType(targetType)
                .createdBy(currentUser)
                .build();

        notificationRepository.save(notification);

        List<NotificationRecipient> recipients = targetApartments.stream()
                .map(apartment -> NotificationRecipient.builder()
                        .notification(notification)
                        .apartment(apartment)
                        .isRead(false)
                        .build())
                .toList();
        recipientRepository.saveAll(recipients);

        return toResponse(notification, false);
    }

    @Override
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(notification -> toResponse(notification, null))
                .toList();
    }

    @Override
    public List<NotificationResponse> getMyNotifications() {
        List<Long> apartmentIds = getCurrentResidentHeadApartmentIds();
        if (apartmentIds.isEmpty()) {
            return List.of();
        }

        return recipientRepository.findByApartment_IdInOrderByNotification_CreatedAtDesc(apartmentIds)
                .stream()
                .map(recipient -> toResponse(recipient.getNotification(), recipient.getIsRead()))
                .toList();
    }

    @Override
    @Transactional
    public List<NotificationTargetResponse> getNotificationTargets() {
        return residentApartmentRepository.findByIsHeadTrueAndIsActiveTrue()
                .stream()
                .map(residency -> new NotificationTargetResponse(
                        residency.getApartment().getId(),
                        residency.getApartment().getRoomNumber(),
                        residency.getResident().getFullName(),
                        residency.getResident().getEmail()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void markMyNotificationsAsRead() {
        List<Long> apartmentIds = getCurrentResidentHeadApartmentIds();
        if (apartmentIds.isEmpty()) {
            return;
        }

        List<NotificationRecipient> unreadRecipients =
                recipientRepository.findByApartment_IdInAndIsReadFalse(apartmentIds);
        unreadRecipients.forEach(recipient -> recipient.setIsRead(true));
        recipientRepository.saveAll(unreadRecipients);
    }

    private String normalizeTargetType(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            return TARGET_ALL;
        }

        String normalized = targetType.trim().toUpperCase();
        if (!TARGET_ALL.equals(normalized) && !TARGET_SPECIFIC.equals(normalized)) {
            throw new IllegalArgumentException("targetType chỉ được là ALL hoặc SPECIFIC");
        }
        return normalized;
    }

    private List<Apartment> resolveTargetApartments(String targetType, List<Long> apartmentIds) {
        if (TARGET_ALL.equals(targetType)) {
            return residentApartmentRepository.findByIsHeadTrueAndIsActiveTrue()
                    .stream()
                    .map(ResidentApartment::getApartment)
                    .distinct()
                    .toList();
        }

        if (apartmentIds == null || apartmentIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một căn hộ nhận thông báo");
        }

        List<Long> uniqueApartmentIds = apartmentIds.stream().distinct().toList();
        List<Apartment> apartments = apartmentRepository.findAllById(uniqueApartmentIds);
        if (apartments.size() != uniqueApartmentIds.size()) {
            throw new NotFoundException("Một hoặc nhiều căn hộ nhận thông báo không tồn tại");
        }
        List<Apartment> headApartments = residentApartmentRepository
                .findByApartment_IdInAndIsHeadTrueAndIsActiveTrue(uniqueApartmentIds)
                .stream()
                .map(ResidentApartment::getApartment)
                .distinct()
                .toList();
        if (headApartments.size() != uniqueApartmentIds.size()) {
            throw new IllegalArgumentException("Mot hoac nhieu can ho chua co chu ho dang hoat dong");
        }

        return headApartments;
    }

    private List<Long> getCurrentResidentHeadApartmentIds() {
        String username = SecurityUtils.getCurrentUsername();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));
        Resident resident = residentRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ cư dân cho tài khoản hiện tại"));

        return residentApartmentRepository.findByResident_IdAndIsHeadTrueAndIsActiveTrue(resident.getId())
                .stream()
                .map(ResidentApartment::getApartment)
                .map(Apartment::getId)
                .distinct()
                .toList();
    }

    private NotificationResponse toResponse(Notification notification, Boolean isRead) {
        List<String> roomNumbers = recipientRepository.findByNotification_Id(notification.getId())
                .stream()
                .map(NotificationRecipient::getApartment)
                .map(Apartment::getRoomNumber)
                .distinct()
                .toList();
        String targetSummary = roomNumbers.isEmpty()
                ? translateTargetType(notification.getTargetType())
                : "Căn " + String.join(", ", roomNumbers);

        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetType(),
                targetSummary,
                roomNumbers,
                isRead,
                notification.getCreatedAt()
        );
    }

    private String translateTargetType(String targetType) {
        if (TARGET_SPECIFIC.equals(targetType)) {
            return "Căn hộ được chọn";
        }
        return "Tất cả chủ hộ";
    }
}
