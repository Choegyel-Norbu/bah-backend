package com.attirehub.notification.service;

import com.attirehub.notification.dto.NotificationResponse;
import com.attirehub.notification.entity.Notification;
import com.attirehub.notification.repository.NotificationRepository;
import com.attirehub.shared.dto.PagedResponse;
import com.attirehub.shared.enums.NotificationType;
import com.attirehub.shared.enums.OrderStatus;
import com.attirehub.shared.enums.Role;
import com.attirehub.shared.exception.ResourceNotFoundException;
import com.attirehub.user.entity.User;
import com.attirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final String REFERENCE_TYPE_ORDER = "ORDER";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void createForNewOrder(Long orderId, Long userId, String orderNumber, String totalMessage) {
        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<User> admins = userRepository.findByRole(Role.ADMIN);
        if (admins.isEmpty()) {
            log.warn("No admin users found when creating notification for new order: orderNumber={}", orderNumber);
            return;
        }

        String customerIdentifier = customer.getEmail();
        String title = "New order received";
        String message = String.format("New order %s from %s. %s", orderNumber, customerIdentifier, totalMessage);

        List<Notification> notifications = admins.stream()
                .map(admin -> Notification.builder()
                        .user(admin)
                        .type(NotificationType.NEW_ORDER)
                        .title(title)
                        .message(message)
                        .referenceType(REFERENCE_TYPE_ORDER)
                        .referenceId(orderNumber)
                        .read(false)
                        .build())
                .toList();

        notificationRepository.saveAll(notifications);
        log.debug("Admin notifications created for new order: orderNumber={}, adminCount={}", orderNumber, admins.size());
    }

    @Override
    @Transactional
    public void createForOrderStatusUpdate(Long customerUserId, String orderNumber, OrderStatus newStatus, String optionalNotes) {
        User customer = userRepository.findById(customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", customerUserId));

        String title = titleForStatus(newStatus);
        String message = String.format("Your order %s is now %s.", orderNumber, newStatus.name().toLowerCase().replace('_', ' '));
        if (optionalNotes != null && !optionalNotes.isBlank()) {
            message += " " + optionalNotes.trim();
        }

        Notification notification = Notification.builder()
                .user(customer)
                .type(NotificationType.ORDER_STATUS_UPDATE)
                .title(title)
                .message(message)
                .referenceType(REFERENCE_TYPE_ORDER)
                .referenceId(orderNumber)
                .read(false)
                .build();
        notificationRepository.save(notification);
        log.debug("Order status notification created: orderNumber={}, status={}, userId={}", orderNumber, newStatus, customerUserId);
    }

    private static String titleForStatus(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> "Order confirmed";
            case PROCESSING -> "Order is being processed";
            case SHIPPED -> "Order shipped";
            case DELIVERED -> "Order delivered";
            case CANCELLED -> "Order cancelled";
            case RETURNED -> "Order returned";
            default -> "Order status updated";
        };
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getByUserId(Long userId, int page, int size,
                                                          Boolean readFilter, NotificationType typeFilter) {
        Page<Notification> notificationPage;
        PageRequest pageRequest = PageRequest.of(page, size);

        if (Boolean.TRUE.equals(readFilter) || Boolean.FALSE.equals(readFilter)) {
            notificationPage = typeFilter != null
                    ? notificationRepository.findByUserIdAndReadAndTypeOrderByCreatedAtDesc(userId, readFilter, typeFilter, pageRequest)
                    : notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, readFilter, pageRequest);
        } else if (typeFilter != null) {
            notificationPage = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, typeFilter, pageRequest);
        } else {
            notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);
        }

        List<NotificationResponse> content = notificationPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PagedResponse.<NotificationResponse>builder()
                .content(content)
                .page(notificationPage.getNumber())
                .size(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .last(notificationPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getAllForAdmin(int page, int size,
                                                            Boolean readFilter, NotificationType typeFilter) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Notification> notificationPage;

        if (Boolean.TRUE.equals(readFilter) || Boolean.FALSE.equals(readFilter)) {
            notificationPage = typeFilter != null
                    ? notificationRepository.findAllByReadAndTypeOrderByCreatedAtDesc(readFilter, typeFilter, pageRequest)
                    : notificationRepository.findAllByReadOrderByCreatedAtDesc(readFilter, pageRequest);
        } else if (typeFilter != null) {
            notificationPage = notificationRepository.findAllByTypeOrderByCreatedAtDesc(typeFilter, pageRequest);
        } else {
            notificationPage = notificationRepository.findAllByOrderByCreatedAtDesc(pageRequest);
        }

        List<NotificationResponse> content = notificationPage.getContent().stream()
                .map(this::toResponseForAdmin)
                .toList();

        return PagedResponse.<NotificationResponse>builder()
                .content(content)
                .page(notificationPage.getNumber())
                .size(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .last(notificationPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", "id", notificationId);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return toResponse(notification);
    }

    @Override
    @Transactional
    public NotificationResponse markAsReadByAdmin(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        notification.setRead(true);
        notificationRepository.save(notification);
        return toResponseForAdmin(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        log.debug("Marked {} notifications as read for userId={}", unread.size(), userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    /** Like toResponse but includes recipient userId and userEmail for admin list. */
    private NotificationResponse toResponseForAdmin(Notification n) {
        User user = n.getUser();
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .build();
    }
}
