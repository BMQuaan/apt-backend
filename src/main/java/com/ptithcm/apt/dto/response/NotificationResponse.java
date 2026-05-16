package com.ptithcm.apt.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String title,
        String content,
        String targetType,
        Boolean isRead,
        LocalDateTime createdAt
) {
}
