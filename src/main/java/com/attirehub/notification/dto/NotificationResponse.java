package com.attirehub.notification.dto;

import com.attirehub.shared.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceType;
    private String referenceId;
    private boolean read;
    private LocalDateTime createdAt;

    /** Set only when listing as admin; identifies the notification recipient. */
    private Long userId;
    /** Set only when listing as admin; recipient email for display. */
    private String userEmail;
}
