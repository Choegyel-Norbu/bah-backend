package com.attirehub.shared.enums;

/**
 * Order lifecycle (PRD Part 3 Flow 4).
 * <p>
 * Stripe: PENDING_PAYMENT → CONFIRMED → … → DELIVERED → (14d) → COMPLETED.
 * COD legacy: PENDING is used as placed/awaiting fulfillment.
 */
public enum OrderStatus {
    PENDING,
    PENDING_PAYMENT,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    RETURNED,
    RETURN_REQUESTED,
    RETURN_APPROVED,
    RETURN_DENIED,
    REFUND_INITIATED,
    REFUNDED
}
