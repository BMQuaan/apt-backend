package com.ptithcm.apt.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationResponse(
        Long id,
        String title,
        String content,
        String targetType,
        String targetSummary,
        List<String> roomNumbers,
        Boolean isRead,
        LocalDateTime createdAt
) {
}
