package com.attirehub.payment.service;

import java.math.BigDecimal;
import java.util.Optional;

public interface StripePaymentService {

    boolean isConfigured();

    /**
     * Creates a PaymentIntent for the given order totals (PRD: idempotency by order number).
     */
    StripeCheckoutIntent createPaymentIntent(
            String orderNumber,
            BigDecimal chargedAmountMajor,
            String chargedCurrencyIso
    );

    void cancelPaymentIntentIfPresent(String paymentIntentId);

    /** PRD 06 — webhook delay reconciliation via Stripe API. */
    Optional<String> retrievePaymentIntentStatus(String paymentIntentId);
}
