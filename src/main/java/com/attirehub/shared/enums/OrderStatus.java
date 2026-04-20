package com.attirehub.shared.enums;

/**
 * Order lifecycle status transitions:
 * PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
 *                    ↘ CANCELLED
 *                                            ↘ RETURNED
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED
}
