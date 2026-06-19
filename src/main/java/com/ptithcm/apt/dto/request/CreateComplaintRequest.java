package com.ptithcm.apt.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateComplaintRequest(
        Long apartmentId,

        @NotBlank(message = "Loại khiếu nại không được để trống")
        String category,

        @NotBlank(message = "Tiêu đề khiếu nại không được để trống")
        @Size(max = 255, message = "Tiêu đề khiếu nại không được vượt quá 255 ký tự")
        String title,

        @NotBlank(message = "Nội dung khiếu nại không được để trống")
        String content
) {
}
