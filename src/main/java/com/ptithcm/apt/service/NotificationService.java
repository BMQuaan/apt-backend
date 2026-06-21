package com.ptithcm.apt.service;

import com.ptithcm.apt.dto.request.CreateNotificationRequest;
import com.ptithcm.apt.dto.response.NotificationResponse;
import com.ptithcm.apt.dto.response.NotificationTargetResponse;

import java.util.List;

public interface NotificationService {
    NotificationResponse createNotification(CreateNotificationRequest request);

    List<NotificationResponse> getAllNotifications();

    List<NotificationResponse> getMyNotifications();

    List<NotificationTargetResponse> getNotificationTargets();

    void markMyNotificationsAsRead();

    void markMyNotificationAsRead(Long notificationId);
}
