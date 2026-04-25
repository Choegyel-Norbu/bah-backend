package com.attirehub.payment.webhook;

import com.attirehub.commission.entity.CommissionLedgerEntry;
import com.attirehub.commission.enums.CommissionEntryType;
import com.attirehub.commission.enums.CommissionStakeholder;
import com.attirehub.commission.repository.CommissionLedgerRepository;
import com.attirehub.notification.service.NotificationService;
import com.attirehub.order.entity.Order;
import com.attirehub.order.entity.OrderItem;
import com.attirehub.order.entity.OrderStatusHistory;
import com.attirehub.order.repository.OrderRepository;
import com.attirehub.partner.entity.Partner;
import com.attirehub.payment.entity.PaymentRecord;
import com.attirehub.payment.enums.PaymentRecordStatus;
import com.attirehub.payment.repository.PaymentRecordRepository;
import com.attirehub.product.entity.ProductVariant;
import com.attirehub.product.repository.ProductVariantRepository;
import com.attirehub.shared.enums.OrderStatus;
import com.attirehub.shared.enums.PaymentStatus;
import com.attirehub.shared.enums.ReferralType;
import com.attirehub.shared.enums.SourcingType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PRD 04 — per-order-item commission ledger (platform, partner; vendor when consignment).
 */
@Service
@RequiredArgsConstructor
public class StripeWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookHandler.class);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PaymentRecordRepository paymentRecordRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CommissionLedgerRepository commissionLedgerRepository;
    private final NotificationService notificationService;

    @Transactional
    public void handlePaymentIntentSucceeded(String paymentIntentId) {
        PaymentRecord record = paymentRecordRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElse(null);
        if (record == null) {
            log.warn("payment_intent.succeeded: no payment record for pi={}", paymentIntentId);
            return;
        }
        if (record.getStatus() == PaymentRecordStatus.SUCCEEDED) {
            return;
        }
        Order order = record.getOrder();
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.info("payment_intent.succeeded: order {} not pending payment (status={})",
                    order.getOrderNumber(), order.getStatus());
            record.setStatus(PaymentRecordStatus.SUCCEEDED);
            paymentRecordRepository.save(record);
            return;
        }

        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getVariant();
            int newReserved = variant.getReservedQuantity() - item.getQuantity();
            if (newReserved < 0) {
                throw new IllegalStateException("Reserved stock underflow for variant " + variant.getId());
            }
            variant.setReservedQuantity(newReserved);
            productVariantRepository.save(variant);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.getStatusHistory().add(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CONFIRMED)
                .notes("Payment confirmed (Stripe)")
                .changedBy(null)
                .build());

        record.setStatus(PaymentRecordStatus.SUCCEEDED);
        paymentRecordRepository.save(record);
        orderRepository.save(order);

        recordCommissionEntries(order);

        String totalMessage = "Total: " + order.getTotal();
        notificationService.createForNewOrder(order.getId(), order.getUser().getId(), order.getOrderNumber(), totalMessage);

        log.info("Stripe payment succeeded: orderNumber={}, pi={}", order.getOrderNumber(), paymentIntentId);
    }

    private void recordCommissionEntries(Order order) {
        Partner partner = order.getReferralPartner();
        BigDecimal partnerPct = (partner != null && partner.getCommissionRatePercent() != null)
                ? partner.getCommissionRatePercent()
                : BigDecimal.ZERO;

        boolean hasReferral = partner != null
                && (order.getReferralType() == ReferralType.HOTEL || order.getReferralType() == ReferralType.GUIDE);

        for (OrderItem item : order.getItems()) {
            BigDecimal line = item.getTotalPrice();
            BigDecimal partnerCut = BigDecimal.ZERO;
            if (hasReferral && partnerPct.compareTo(BigDecimal.ZERO) > 0) {
                partnerCut = line.multiply(partnerPct).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
            }

            if (item.getSourcingType() == SourcingType.CONSIGNMENT && item.getConsignmentCommissionRate() != null) {
                BigDecimal platTakeRate = item.getConsignmentCommissionRate();
                BigDecimal vendorAmt = line.multiply(BigDecimal.ONE.subtract(platTakeRate)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal platformFromLine = line.multiply(platTakeRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal platformNet = platformFromLine.subtract(partnerCut);
                if (platformNet.compareTo(BigDecimal.ZERO) < 0) {
                    platformNet = BigDecimal.ZERO;
                }

                commissionLedgerRepository.save(CommissionLedgerEntry.builder()
                        .order(order)
                        .orderItem(item)
                        .stakeholder(CommissionStakeholder.VENDOR)
                        .entryType(CommissionEntryType.EARNED)
                        .amountBtn(vendorAmt)
                        .commissionRateSnapshot(BigDecimal.ONE.subtract(platTakeRate))
                        .baseAmountBtn(line)
                        .description("Consignment vendor share (pending settlement)")
                        .build());

                commissionLedgerRepository.save(CommissionLedgerEntry.builder()
                        .order(order)
                        .orderItem(item)
                        .stakeholder(CommissionStakeholder.PLATFORM)
                        .entryType(CommissionEntryType.EARNED)
                        .amountBtn(platformNet)
                        .commissionRateSnapshot(platTakeRate)
                        .baseAmountBtn(line)
                        .description("Platform share after partner referral (consignment)")
                        .build());
            } else {
                BigDecimal platformAmt = line.subtract(partnerCut);
                commissionLedgerRepository.save(CommissionLedgerEntry.builder()
                        .order(order)
                        .orderItem(item)
                        .stakeholder(CommissionStakeholder.PLATFORM)
                        .entryType(CommissionEntryType.EARNED)
                        .amountBtn(platformAmt)
                        .commissionRateSnapshot(BigDecimal.ONE)
                        .baseAmountBtn(line)
                        .description("Owned inventory — platform")
                        .build());
            }

            if (partnerCut.compareTo(BigDecimal.ZERO) > 0 && partner != null) {
                CommissionStakeholder ph = order.getReferralType() == ReferralType.HOTEL
                        ? CommissionStakeholder.HOTEL_PARTNER
                        : CommissionStakeholder.GUIDE_PARTNER;
                commissionLedgerRepository.save(CommissionLedgerEntry.builder()
                        .order(order)
                        .orderItem(item)
                        .partner(partner)
                        .stakeholder(ph)
                        .entryType(CommissionEntryType.EARNED)
                        .amountBtn(partnerCut)
                        .commissionRateSnapshot(partnerPct.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP))
                        .baseAmountBtn(line)
                        .description("Partner referral commission (pending settlement)")
                        .build());
            }
        }
    }

    @Transactional
    public void handlePaymentIntentCanceled(String paymentIntentId) {
        PaymentRecord record = paymentRecordRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElse(null);
        if (record == null) {
            return;
        }
        Order order = record.getOrder();
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            record.setStatus(PaymentRecordStatus.CANCELLED);
            paymentRecordRepository.save(record);
            return;
        }
        releaseReservationAndCancelOrder(order, record, "Stripe PaymentIntent canceled");
    }

    private void releaseReservationAndCancelOrder(Order order, PaymentRecord record, String notes) {
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            variant.setReservedQuantity(variant.getReservedQuantity() - item.getQuantity());
            if (variant.getReservedQuantity() < 0) {
                variant.setReservedQuantity(0);
            }
            productVariantRepository.save(variant);
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.getStatusHistory().add(OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CANCELLED)
                .notes(notes)
                .changedBy(null)
                .build());
        record.setStatus(PaymentRecordStatus.CANCELLED);
        paymentRecordRepository.save(record);
        orderRepository.save(order);
        notificationService.createForOrderStatusUpdate(
                order.getUser().getId(), order.getOrderNumber(), OrderStatus.CANCELLED, notes);
        log.info("Released reservation for order {} ({})", order.getOrderNumber(), notes);
    }
}
