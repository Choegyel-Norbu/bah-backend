package com.attirehub.inventory.enums;

public enum StockChangeType {
    RESERVATION,
    RESERVATION_RELEASE,
    SALE_CONFIRM,
    CANCELLATION_RESTORE,
    ADJUSTMENT,
    /** Stripe checkout: move units from available → reserved */
    CHECKOUT_RESERVE,
    /** COD checkout: decrement available (no reservation pool) */
    CHECKOUT_DEDUCT
}
