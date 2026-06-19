package com.ptithcm.apt.service;

import com.ptithcm.apt.dto.request.CreateNotificationRequest;
import com.ptithcm.apt.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {
    NotificationResponse createNotification(CreateNotificationRequest request);

    List<NotificationResponse> getAllNotifications();

    List<NotificationResponse> getMyNotifications();

    void markMyNotificationsAsRead();
}
