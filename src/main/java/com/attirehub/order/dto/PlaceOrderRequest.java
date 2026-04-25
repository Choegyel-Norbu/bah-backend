package com.attirehub.order.dto;

import com.attirehub.shared.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {

    @NotNull(message = "Shipping address ID is required")
    private Long shippingAddressId;

    private String couponCode;
    private String notes;

    /**
     * Defaults to cash-on-delivery for backward compatibility. Use {@link PaymentMethod#STRIPE} for card checkout.
     */
    private PaymentMethod paymentMethod;

    /** Optional partner referral code from cookie / localStorage (PRD Flow 2). */
    private String referralCode;

    /**
     * Locked at checkout for Stripe: order totals are in BTN; this is BTN per 1 unit of {@link #chargedCurrency}
     * (e.g. 84.5 means 1 USD = 84.5 BTN).
     */
    private BigDecimal exchangeRateUsed;

    /** ISO 4217 for Stripe charge (e.g. usd). Defaults to usd when using Stripe. */
    private String chargedCurrency;
}
