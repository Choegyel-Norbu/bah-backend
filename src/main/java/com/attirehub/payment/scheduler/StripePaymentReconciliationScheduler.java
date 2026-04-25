package com.attirehub.payment.scheduler;

import com.attirehub.order.entity.Order;
import com.attirehub.order.repository.OrderRepository;
import com.attirehub.payment.service.StripePaymentService;
import com.attirehub.payment.webhook.StripeWebhookHandler;
import com.attirehub.shared.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PRD 06 §1.3 — if Stripe webhook is delayed, reconcile from PaymentIntent status.
 */
@Component
@RequiredArgsConstructor
public class StripePaymentReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentReconciliationScheduler.class);

    private final OrderRepository orderRepository;
    private final StripePaymentService stripePaymentService;
    private final StripeWebhookHandler stripeWebhookHandler;

    @Scheduled(cron = "${app.order.stripe-reconcile-cron:0 */10 * * * *}")
    public void reconcileStalePendingPayments() {
        if (!stripePaymentService.isConfigured()) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        List<Order> stuck = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, cutoff);
        for (Order order : stuck) {
            if (order.getStripePaymentIntentId() == null) {
                continue;
            }
            stripePaymentService.retrievePaymentIntentStatus(order.getStripePaymentIntentId())
                    .filter("succeeded"::equalsIgnoreCase)
                    .ifPresent(status -> {
                        try {
                            stripeWebhookHandler.handlePaymentIntentSucceeded(order.getStripePaymentIntentId());
                            log.info("Reconciled Stripe payment for order {}", order.getOrderNumber());
                        } catch (Exception e) {
                            log.warn("Stripe reconcile failed for order {}: {}", order.getOrderNumber(), e.getMessage());
                        }
                    });
        }
    }
}
