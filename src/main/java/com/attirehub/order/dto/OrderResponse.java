package com.attirehub.order.dto;

import com.attirehub.shared.enums.OrderStatus;
import com.attirehub.shared.enums.PaymentMethod;
import com.attirehub.shared.enums.PaymentStatus;
import com.attirehub.shared.enums.ReferralType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal shippingCost;
    private BigDecimal total;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String couponCode;
    private String notes;

    private ReferralType referralType;
    private String referralCode;
    private String referralPartnerDisplayName;
    private BigDecimal exchangeRateUsed;
    private String stripePaymentIntentId;
    private String chargedCurrency;
    private String trackingNumber;
    private LocalDateTime deliveredAt;

    /**
     * Returned only when creating a Stripe checkout; omitted on later reads.
     */
    private String stripeClientSecret;

    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    /** Customer details (included only in admin order responses). */
    private OrderCustomerSummary customer;
    /** Shipping address for the order. */
    private ShippingAddressSummary shippingAddress;
}
