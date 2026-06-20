package com.ptithcm.apt.controller;

import com.ptithcm.apt.dto.request.CreateNotificationRequest;
import com.ptithcm.apt.dto.response.ApiResponse;
import com.ptithcm.apt.dto.response.NotificationResponse;
import com.ptithcm.apt.dto.response.NotificationTargetResponse;
import com.ptithcm.apt.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.createNotification(request),
                "Tạo thông báo thành công"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAllNotifications() {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getAllNotifications(),
                "Lấy danh sách thông báo quản trị thành công"));
    }

    @GetMapping("/targets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationTargetResponse>>> getNotificationTargets() {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getNotificationTargets(),
                "Lay danh sach chu ho nhan thong bao thanh cong"));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications() {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getMyNotifications(),
                "Lấy danh sách thông báo thành công"));
    }

    @PatchMapping("/my/read-all")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markMyNotificationsAsRead() {
        notificationService.markMyNotificationsAsRead();
        return ResponseEntity.ok(ApiResponse.success(null, "Đã đánh dấu tất cả thông báo là đã đọc"));
    }
}
