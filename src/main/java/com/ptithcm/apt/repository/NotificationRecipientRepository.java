package com.ptithcm.apt.repository;

import com.ptithcm.apt.entity.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {
    List<NotificationRecipient> findByNotification_Id(Long notificationId);

    List<NotificationRecipient> findByApartment_IdInOrderByNotification_CreatedAtDesc(List<Long> apartmentIds);

    List<NotificationRecipient> findByApartment_IdInAndIsReadFalse(List<Long> apartmentIds);
}
