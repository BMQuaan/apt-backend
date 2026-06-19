package com.ptithcm.apt.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateNotificationRequest(
        @NotBlank(message = "Tiêu đề thông báo không được để trống")
        @Size(max = 255, message = "Tiêu đề thông báo không được vượt quá 255 ký tự")
        String title,

        @NotBlank(message = "Nội dung thông báo không được để trống")
        String content,

        String targetType,

        List<Long> apartmentIds
) {
}
