package com.attirehub.payment.webhook;

import com.attirehub.payment.config.StripeProperties;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.function.Consumer;

@RestController
@RequiredArgsConstructor
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeProperties stripeProperties;
    private final StripeWebhookHandler stripeWebhookHandler;

    @PostMapping(value = "/api/v1/webhooks/stripe", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        if (stripeProperties.getWebhookSecret() == null || stripeProperties.getWebhookSecret().isBlank()) {
            log.warn("Stripe webhook received but app.stripe.webhook-secret is not configured");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("webhook not configured");
        }
        if (sigHeader == null || sigHeader.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("missing signature");
        }
        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid signature");
        }

        try {
            switch (event.getType()) {
                case "payment_intent.succeeded" -> dispatchPaymentIntent(event, pi ->
                        stripeWebhookHandler.handlePaymentIntentSucceeded(pi.getId()));
                case "payment_intent.canceled" -> dispatchPaymentIntent(event, pi ->
                        stripeWebhookHandler.handlePaymentIntentCanceled(pi.getId()));
                default -> log.debug("Ignoring Stripe event type {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Stripe webhook handler failed for type {}", event.getType(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("handler error");
        }
        return ResponseEntity.ok("ok");
    }

    private void dispatchPaymentIntent(Event event, Consumer<PaymentIntent> consumer) {
        Optional<StripeObject> object = event.getDataObjectDeserializer().getObject();
        if (object.isPresent() && object.get() instanceof PaymentIntent pi) {
            consumer.accept(pi);
            return;
        }
        log.warn("Could not deserialize PaymentIntent from event {}", event.getId());
    }
}
