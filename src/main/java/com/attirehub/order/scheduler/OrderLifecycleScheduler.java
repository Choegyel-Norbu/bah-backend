package com.attirehub.order.scheduler;

import com.attirehub.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderLifecycleScheduler {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${app.order.payment-timeout-check-ms:60000}")
    public void expireStaleStripeCheckouts() {
        orderService.expireStalePendingPaymentOrders();
    }

    @Scheduled(cron = "${app.order.delivered-auto-complete-cron:0 15 * * * *}")
    public void completeDeliveredOrders() {
        orderService.autoCompleteDeliveredOrders();
    }
}
