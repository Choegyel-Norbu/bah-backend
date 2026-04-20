package com.attirehub.notification.repository;

import com.attirehub.notification.entity.Notification;
import com.attirehub.shared.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadOrderByCreatedAtDesc(Long userId, boolean read, Pageable pageable);

    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type, Pageable pageable);

    Page<Notification> findByUserIdAndReadAndTypeOrderByCreatedAtDesc(Long userId, boolean read, NotificationType type, Pageable pageable);

    List<Notification> findByUserIdAndReadFalse(Long userId);

    // Admin: all notifications across users
    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Notification> findAllByReadOrderByCreatedAtDesc(boolean read, Pageable pageable);
    Page<Notification> findAllByTypeOrderByCreatedAtDesc(NotificationType type, Pageable pageable);
    Page<Notification> findAllByReadAndTypeOrderByCreatedAtDesc(boolean read, NotificationType type, Pageable pageable);
}
