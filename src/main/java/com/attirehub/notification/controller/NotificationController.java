package com.attirehub.notification.controller;

import com.attirehub.notification.dto.NotificationResponse;
import com.attirehub.notification.service.NotificationService;
import com.attirehub.shared.dto.ApiResponse;
import com.attirehub.shared.dto.PagedResponse;
import com.attirehub.shared.enums.NotificationType;
import com.attirehub.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for the authenticated user's in-app notifications.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) NotificationType type) {
        PagedResponse<NotificationResponse> result = notificationService.getByUserId(
                currentUser.getId(), page, size, read, type);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long notificationId) {
        NotificationResponse updated = notificationService.markAsRead(currentUser.getId(), notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", updated));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal User currentUser) {
        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }
}
