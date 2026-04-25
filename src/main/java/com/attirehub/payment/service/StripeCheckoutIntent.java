package com.attirehub.payment.service;

public record StripeCheckoutIntent(
        String paymentIntentId,
        String clientSecret,
        long amountMinor,
        String currency
) {
}
