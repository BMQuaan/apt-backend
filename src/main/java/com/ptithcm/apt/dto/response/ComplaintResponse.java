package com.ptithcm.apt.dto.response;

import java.time.LocalDateTime;

public record ComplaintResponse(
        Long id,
        String category,
        String title,
        String content,
        String status,
        Long apartmentId,
        String roomNumber,
        Long residentId,
        String residentName,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
}
