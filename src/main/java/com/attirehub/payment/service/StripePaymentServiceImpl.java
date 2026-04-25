package com.attirehub.payment.service;

import com.attirehub.payment.config.StripeProperties;
import com.attirehub.shared.exception.BadRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StripePaymentServiceImpl implements StripePaymentService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentServiceImpl.class);

    private final StripeProperties stripeProperties;

    @Override
    public boolean isConfigured() {
        return stripeProperties.getSecretKey() != null && !stripeProperties.getSecretKey().isBlank();
    }

    @Override
    public StripeCheckoutIntent createPaymentIntent(
            String orderNumber,
            BigDecimal chargedAmountMajor,
            String chargedCurrencyIso) {
        if (!isConfigured()) {
            throw new BadRequestException("Stripe is not configured (missing app.stripe.secret-key)");
        }
        if (chargedAmountMajor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Charged amount must be positive");
        }
        String currency = chargedCurrencyIso.toLowerCase(Locale.ROOT);
        if (currency.length() != 3) {
            throw new BadRequestException("chargedCurrency must be a 3-letter ISO code");
        }
        BigDecimal normalized = chargedAmountMajor.setScale(2, RoundingMode.HALF_UP);
        long amountMinor = normalized.movePointRight(2).longValueExact();
        if (amountMinor <= 0) {
            throw new BadRequestException("Stripe amount is too small after conversion");
        }

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountMinor)
                    .setCurrency(currency)
                    .putMetadata("orderNumber", orderNumber)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setApiKey(stripeProperties.getSecretKey())
                    .setIdempotencyKey(orderNumber)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params, options);
            return new StripeCheckoutIntent(intent.getId(), intent.getClientSecret(), amountMinor, currency);
        } catch (StripeException e) {
            log.error("Stripe PaymentIntent failed for orderNumber={}", orderNumber, e);
            throw new BadRequestException("Could not start payment: " + e.getMessage());
        } catch (ArithmeticException e) {
            throw new BadRequestException("Invalid charged amount for Stripe minor units");
        }
    }

    @Override
    public void cancelPaymentIntentIfPresent(String paymentIntentId) {
        if (paymentIntentId == null || paymentIntentId.isBlank() || !isConfigured()) {
            return;
        }
        try {
            PaymentIntent intent = PaymentIntent.retrieve(
                    paymentIntentId,
                    RequestOptions.builder().setApiKey(stripeProperties.getSecretKey()).build()
            );
            if ("canceled".equalsIgnoreCase(intent.getStatus()) || "succeeded".equalsIgnoreCase(intent.getStatus())) {
                return;
            }
            intent.cancel(RequestOptions.builder().setApiKey(stripeProperties.getSecretKey()).build());
        } catch (StripeException e) {
            log.warn("Could not cancel Stripe PaymentIntent {}: {}", paymentIntentId, e.getMessage());
        }
    }

    @Override
    public Optional<String> retrievePaymentIntentStatus(String paymentIntentId) {
        if (paymentIntentId == null || paymentIntentId.isBlank() || !isConfigured()) {
            return Optional.empty();
        }
        try {
            PaymentIntent intent = PaymentIntent.retrieve(
                    paymentIntentId,
                    RequestOptions.builder().setApiKey(stripeProperties.getSecretKey()).build());
            return Optional.ofNullable(intent.getStatus());
        } catch (StripeException e) {
            log.debug("Could not retrieve PaymentIntent {}: {}", paymentIntentId, e.getMessage());
            return Optional.empty();
        }
    }
}
