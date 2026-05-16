package com.ptithcm.apt.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateComplaintStatusRequest(
        @NotBlank(message = "Trạng thái khiếu nại không được để trống")
        String status
) {
}
