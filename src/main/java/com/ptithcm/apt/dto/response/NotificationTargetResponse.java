package com.ptithcm.apt.dto.response;

public record NotificationTargetResponse(
        Long apartmentId,
        String roomNumber,
        String residentName,
        String residentEmail
) {
}
