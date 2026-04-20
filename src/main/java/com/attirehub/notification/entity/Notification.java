package com.attirehub.notification.entity;

import com.attirehub.shared.entity.BaseEntity;
import com.attirehub.shared.enums.NotificationType;
import com.attirehub.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * Stored in-app notification. Created when domain events occur (e.g. new order placed).
 * Users can list their notifications and mark them as read.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "varchar(30)")
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "reference_type", nullable = false, length = 50)
    private String referenceType;

    @Column(name = "reference_id", nullable = false, length = 100)
    private String referenceId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;
}
