package com.attirehub.notification.controller;

import com.attirehub.notification.dto.NotificationResponse;
import com.attirehub.notification.service.NotificationService;
import com.attirehub.shared.dto.ApiResponse;
import com.attirehub.shared.dto.PagedResponse;
import com.attirehub.shared.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only endpoint to fetch all notifications across all users and types.
 * Secured by /api/v1/admin/** in SecurityConfig and @PreAuthorize.
 */
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationService notificationService;

    /**
     * Get all notifications (all users, all types). Optional filters by read status and type.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) NotificationType type) {
        PagedResponse<NotificationResponse> result = notificationService.getAllForAdmin(page, size, read, type);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Mark any notification as read (by id). Use when admin is viewing the full list and marks one read.
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Long notificationId) {
        NotificationResponse updated = notificationService.markAsReadByAdmin(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", updated));
    }
}
