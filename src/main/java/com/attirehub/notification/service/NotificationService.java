package com.attirehub.notification.service;

import com.attirehub.notification.dto.NotificationResponse;
import com.attirehub.shared.dto.PagedResponse;
import com.attirehub.shared.enums.NotificationType;
import com.attirehub.shared.enums.OrderStatus;

/**
 * Service for stored in-app notifications. Notifications are created when domain events
 * occur (e.g. new order, order status update). Users can list and mark notifications as read.
 */
public interface NotificationService {

    /**
     * Creates a notification when a new order is placed. Called by order module.
     *
     * @param orderId    persisted order id
     * @param userId     order owner
     * @param orderNumber order number for display and reference
     * @param totalMessage short message (e.g. total amount) for the notification body
     */
    void createForNewOrder(Long orderId, Long userId, String orderNumber, String totalMessage);

    /**
     * Creates a notification for the customer when their order status is updated (e.g. shipped, delivered).
     * Called by order module when admin changes status or customer cancels.
     *
     * @param customerUserId id of the user who placed the order
     * @param orderNumber     order number for display and reference
     * @param newStatus       the new order status
     * @param optionalNotes   optional admin notes (e.g. tracking info); may be null
     */
    void createForOrderStatusUpdate(Long customerUserId, String orderNumber, OrderStatus newStatus, String optionalNotes);

    /**
     * Returns paginated notifications for the given user, optionally filtered by read status and type.
     */
    PagedResponse<NotificationResponse> getByUserId(Long userId, int page, int size,
                                                    Boolean readFilter, NotificationType typeFilter);

    /**
     * Returns paginated notifications for all users (admin). Includes all types. Optional filters by read and type.
     */
    PagedResponse<NotificationResponse> getAllForAdmin(int page, int size,
                                                       Boolean readFilter, NotificationType typeFilter);

    /**
     * Marks a single notification as read. Only the owner can mark it.
     */
    NotificationResponse markAsRead(Long userId, Long notificationId);

    /**
     * Marks a single notification as read (admin). Can mark any notification by id.
     */
    NotificationResponse markAsReadByAdmin(Long notificationId);

    /**
     * Marks all notifications for the user as read.
     */
    void markAllAsRead(Long userId);
}
