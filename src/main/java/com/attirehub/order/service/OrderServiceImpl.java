package com.attirehub.order.service;

import com.attirehub.cart.entity.Cart;
import com.attirehub.cart.entity.CartItem;
import com.attirehub.cart.repository.CartRepository;
import com.attirehub.order.dto.OrderCustomerSummary;
import com.attirehub.order.dto.OrderItemResponse;
import com.attirehub.order.dto.OrderResponse;
import com.attirehub.order.dto.PlaceOrderRequest;
import com.attirehub.order.dto.ShippingAddressSummary;
import com.attirehub.order.dto.UpdateOrderStatusRequest;
import com.attirehub.order.entity.Order;
import com.attirehub.order.entity.OrderItem;
import com.attirehub.order.entity.OrderStatusHistory;
import com.attirehub.notification.service.NotificationService;
import com.attirehub.order.repository.OrderRepository;
import com.attirehub.inventory.enums.StockChangeType;
import com.attirehub.inventory.service.StockAuditService;
import com.attirehub.partner.enums.PartnerStatus;
import com.attirehub.partner.enums.PartnerType;
import com.attirehub.partner.repository.PartnerRepository;
import com.attirehub.payment.entity.PaymentRecord;
import com.attirehub.payment.enums.PaymentRecordStatus;
import com.attirehub.payment.repository.PaymentRecordRepository;
import com.attirehub.payment.service.StripeCheckoutIntent;
import com.attirehub.payment.service.StripePaymentService;
import com.attirehub.product.entity.Product;
import com.attirehub.product.entity.ProductVariant;
import com.attirehub.product.repository.ProductVariantRepository;
import com.attirehub.shared.dto.PagedResponse;
import com.attirehub.shared.enums.OrderStatus;
import com.attirehub.shared.enums.PaymentMethod;
import com.attirehub.shared.enums.PaymentStatus;
import com.attirehub.shared.enums.ReferralType;
import com.attirehub.shared.exception.BadRequestException;
import com.attirehub.shared.exception.ResourceNotFoundException;
import com.attirehub.user.entity.Address;
import com.attirehub.user.entity.User;
import com.attirehub.user.repository.AddressRepository;
import com.attirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    /** PRD 3.4 — minimum order value */
    private static final BigDecimal MIN_ORDER_TOTAL_BTN = new BigDecimal("500");
    private static final int MAX_LINE_ITEMS = 20;
    private static final int MAX_QTY_PER_LINE = 10;

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository productVariantRepository;
    private final NotificationService notificationService;
    private final PartnerRepository partnerRepository;
    private final StripePaymentService stripePaymentService;
    private final PaymentRecordRepository paymentRecordRepository;
    private final StockAuditService stockAuditService;

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }
        if (cart.getItems().size() > MAX_LINE_ITEMS) {
            throw new BadRequestException("Maximum " + MAX_LINE_ITEMS + " line items per order");
        }
        for (CartItem ci : cart.getItems()) {
            if (ci.getQuantity() > MAX_QTY_PER_LINE) {
                throw new BadRequestException("Maximum quantity per line item is " + MAX_QTY_PER_LINE);
            }
        }

        Address shippingAddress = addressRepository.findByIdAndUserId(request.getShippingAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", request.getShippingAddressId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        PaymentMethod paymentMethod = request.getPaymentMethod() != null
                ? request.getPaymentMethod()
                : PaymentMethod.CASH_ON_DELIVERY;
        boolean stripeCheckout = paymentMethod == PaymentMethod.STRIPE;

        if (stripeCheckout) {
            if (request.getExchangeRateUsed() == null
                    || request.getExchangeRateUsed().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("exchangeRateUsed is required for Stripe checkout (BTN per 1 charged currency unit)");
            }
            if (!stripePaymentService.isConfigured()) {
                throw new BadRequestException("Stripe is not configured on the server");
            }
        }

        String orderNumber = generateOrderNumber();
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .shippingAddress(shippingAddress)
                .couponCode(request.getCouponCode())
                .notes(request.getNotes())
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        applyReferralAttribution(order, request.getReferralCode(), user);
        if (stripeCheckout) {
            order.setExchangeRateUsed(request.getExchangeRateUsed().setScale(6, RoundingMode.HALF_UP));
            String ccy = request.getChargedCurrency() != null && !request.getChargedCurrency().isBlank()
                    ? request.getChargedCurrency().trim().toLowerCase(Locale.ROOT)
                    : "usd";
            if (ccy.length() != 3) {
                throw new BadRequestException("chargedCurrency must be a 3-letter ISO code");
            }
            order.setChargedCurrency(ccy);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = cartItem.getVariant();

            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException(
                        String.format("Insufficient stock for %s (%s/%s). Available: %d, Requested: %d",
                                variant.getProduct().getName(), variant.getSize(), variant.getColor(),
                                variant.getStockQuantity(), cartItem.getQuantity()));
            }

            if (stripeCheckout) {
                variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
                variant.setReservedQuantity(variant.getReservedQuantity() + cartItem.getQuantity());
            } else {
                variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
            }
            productVariantRepository.save(variant);

            stockAuditService.log(
                    variant,
                    stripeCheckout ? StockChangeType.CHECKOUT_RESERVE : StockChangeType.CHECKOUT_DEDUCT,
                    -cartItem.getQuantity(),
                    variant.getStockQuantity(),
                    stripeCheckout ? variant.getReservedQuantity() : null,
                    null,
                    (stripeCheckout ? "Stripe reserve for " : "COD sale for ") + orderNumber);

            BigDecimal variantDiscount = variant.getDiscount() != null ? variant.getDiscount() : BigDecimal.ZERO;
            BigDecimal discountedUnitPrice = variant.getPrice().subtract(variantDiscount);
            if (discountedUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
                discountedUnitPrice = BigDecimal.ZERO;
            }
            discountedUnitPrice = discountedUnitPrice.setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemTotal = discountedUnitPrice
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            Product product = variant.getProduct();
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variant(variant)
                    .productName(product.getName())
                    .sku(variant.getSku())
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(discountedUnitPrice)
                    .totalPrice(itemTotal)
                    .sourcingType(product.getSourcingType())
                    .consignmentCommissionRate(product.getConsignmentCommissionRate())
                    .build();

            order.getItems().add(orderItem);
            subtotal = subtotal.add(itemTotal);
        }

        order.setSubtotal(subtotal);
        order.setTotal(subtotal.subtract(order.getDiscount())
                .add(order.getTax())
                .add(order.getShippingCost()));

        if (order.getTotal().compareTo(MIN_ORDER_TOTAL_BTN) < 0) {
            throw new BadRequestException("Minimum order value is BTN " + MIN_ORDER_TOTAL_BTN);
        }

        if (stripeCheckout) {
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            order.getStatusHistory().add(OrderStatusHistory.builder()
                    .order(order)
                    .status(OrderStatus.PENDING_PAYMENT)
                    .notes("Awaiting Stripe payment")
                    .changedBy(userId)
                    .build());
        } else {
            order.setStatus(OrderStatus.PENDING);
            order.getStatusHistory().add(OrderStatusHistory.builder()
                    .order(order)
                    .status(OrderStatus.PENDING)
                    .notes("Order placed")
                    .changedBy(userId)
                    .build());
        }

        Order savedOrder = orderRepository.save(order);

        String stripeClientSecret = null;
        if (stripeCheckout) {
            BigDecimal chargedMajor = savedOrder.getTotal()
                    .divide(request.getExchangeRateUsed(), 2, RoundingMode.HALF_UP);
            if (chargedMajor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Converted charge amount is invalid; check exchangeRateUsed");
            }
            StripeCheckoutIntent intent = stripePaymentService.createPaymentIntent(
                    savedOrder.getOrderNumber(),
                    chargedMajor,
                    savedOrder.getChargedCurrency()
            );
            stripeClientSecret = intent.clientSecret();

            PaymentRecord payment = PaymentRecord.builder()
                    .order(savedOrder)
                    .stripePaymentIntentId(intent.paymentIntentId())
                    .status(PaymentRecordStatus.CREATED)
                    .amountMinor(intent.amountMinor())
                    .currency(intent.currency())
                    .build();
            paymentRecordRepository.save(payment);

            savedOrder.setStripePaymentIntentId(intent.paymentIntentId());
            orderRepository.save(savedOrder);
        }

        if (!stripeCheckout) {
            String totalMessage = "Total: " + savedOrder.getTotal();
            notificationService.createForNewOrder(savedOrder.getId(), userId, orderNumber, totalMessage);
        }

        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Order placed: orderNumber={}, userId={}, paymentMethod={}, total={}",
                orderNumber, userId, paymentMethod, order.getTotal());
        return mapOrderToResponse(savedOrder, false, stripeClientSecret);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, String orderNumber) {
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return mapOrderToResponse(order, false, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getUserOrders(Long userId, int page, int size, OrderStatus statusFilter) {
        Page<Order> orderPage = statusFilter != null
                ? orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, statusFilter, PageRequest.of(page, size))
                : orderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        List<OrderResponse> content = orderPage.getContent()
                .stream()
                .map(order -> mapOrderToResponse(order, false, null))
                .toList();

        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .last(orderPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long userId, String orderNumber) {
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));

        if (order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.CONFIRMED
                && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Order cannot be cancelled in its current status");
        }

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            restoreReservedStock(order);
            stripePaymentService.cancelPaymentIntentIfPresent(order.getStripePaymentIntentId());
            paymentRecordRepository.findTopByOrder_IdOrderByIdDesc(order.getId()).ifPresent(pr -> {
                pr.setStatus(PaymentRecordStatus.CANCELLED);
                paymentRecordRepository.save(pr);
            });
            order.setPaymentStatus(PaymentStatus.FAILED);
        } else {
            for (OrderItem item : order.getItems()) {
                ProductVariant variant = item.getVariant();
                variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                productVariantRepository.save(variant);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.CANCELLED)
                .notes("Order cancelled by customer")
                .changedBy(userId)
                .build();
        order.getStatusHistory().add(history);

        Order savedOrder = orderRepository.save(order);
        notificationService.createForOrderStatusUpdate(savedOrder.getUser().getId(), orderNumber, OrderStatus.CANCELLED, null);
        log.info("Order cancelled: orderNumber={}, userId={}", orderNumber, userId);
        return mapOrderToResponse(savedOrder, false, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(int page, int size, OrderStatus statusFilter) {
        Page<Order> orderPage = statusFilter != null
                ? orderRepository.findAllByStatusOrderByCreatedAtDesc(statusFilter, PageRequest.of(page, size))
                : orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        List<OrderResponse> content = orderPage.getContent().stream()
                .map(order -> mapOrderToResponse(order, true, null))
                .toList();
        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .last(orderPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long adminUserId, String orderNumber, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        order.setStatus(request.getStatus());

        if (request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()) {
            order.setTrackingNumber(request.getTrackingNumber().trim());
        }
        if (request.getStatus() == OrderStatus.DELIVERED && order.getDeliveredAt() == null) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(request.getStatus())
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .changedBy(adminUserId)
                .build();
        order.getStatusHistory().add(history);
        Order saved = orderRepository.save(order);
        notificationService.createForOrderStatusUpdate(saved.getUser().getId(), orderNumber, request.getStatus(), request.getNotes());
        log.info("Order status updated: orderNumber={}, newStatus={}, adminUserId={}", orderNumber, request.getStatus(), adminUserId);
        return mapOrderToResponse(saved, true, null);
    }

    @Override
    @Transactional
    public void expireStalePendingPaymentOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<Order> stale = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, cutoff);
        for (Order order : stale) {
            restoreReservedStock(order);
            stripePaymentService.cancelPaymentIntentIfPresent(order.getStripePaymentIntentId());
            paymentRecordRepository.findTopByOrder_IdOrderByIdDesc(order.getId()).ifPresent(pr -> {
                pr.setStatus(PaymentRecordStatus.CANCELLED);
                paymentRecordRepository.save(pr);
            });
            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.getStatusHistory().add(OrderStatusHistory.builder()
                    .order(order)
                    .status(OrderStatus.CANCELLED)
                    .notes("Payment timeout (30 minutes)")
                    .changedBy(null)
                    .build());
            orderRepository.save(order);
            notificationService.createForOrderStatusUpdate(
                    order.getUser().getId(), order.getOrderNumber(), OrderStatus.CANCELLED, "Payment timeout");
            log.info("Expired pending-payment order: {}", order.getOrderNumber());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse trackOrder(String orderNumber, String email) {
        Order order = orderRepository.findByOrderNumber(orderNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        if (!order.getUser().getEmail().equalsIgnoreCase(email.trim())) {
            throw new BadRequestException("Email does not match this order");
        }
        return mapOrderToResponse(order, false, null);
    }

    @Override
    @Transactional
    public void autoCompleteDeliveredOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(14);
        List<Order> orders = orderRepository.findByStatusAndDeliveredAtBefore(OrderStatus.DELIVERED, deadline);
        for (Order order : orders) {
            order.setStatus(OrderStatus.COMPLETED);
            order.getStatusHistory().add(OrderStatusHistory.builder()
                    .order(order)
                    .status(OrderStatus.COMPLETED)
                    .notes("Auto-completed 14 days after delivery")
                    .changedBy(null)
                    .build());
            orderRepository.save(order);
            notificationService.createForOrderStatusUpdate(
                    order.getUser().getId(), order.getOrderNumber(), OrderStatus.COMPLETED, null);
            log.info("Auto-completed order after delivery window: {}", order.getOrderNumber());
        }
    }

    private void applyReferralAttribution(Order order, String rawCode, User purchaser) {
        order.setReferralType(ReferralType.NONE);
        order.setReferralCode(null);
        order.setReferralPartner(null);
        order.setReferralPartnerDisplayName(null);
        if (rawCode == null || rawCode.isBlank()) {
            return;
        }
        String code = rawCode.trim();
        partnerRepository.findByReferralCodeIgnoreCaseAndStatus(code, PartnerStatus.ACTIVE).ifPresent(partner -> {
            if (partner.getEmail() != null && !partner.getEmail().isBlank()
                    && purchaser.getEmail() != null
                    && partner.getEmail().trim().equalsIgnoreCase(purchaser.getEmail().trim())) {
                log.info("Self-referral blocked: partner {} matches purchaser email", partner.getReferralCode());
                return;
            }
            order.setReferralType(partner.getPartnerType() == PartnerType.HOTEL
                    ? ReferralType.HOTEL
                    : ReferralType.GUIDE);
            order.setReferralCode(partner.getReferralCode());
            order.setReferralPartner(partner);
            order.setReferralPartnerDisplayName(partner.getDisplayName());
        });
    }

    private void restoreReservedStock(Order order) {
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            variant.setReservedQuantity(variant.getReservedQuantity() - item.getQuantity());
            if (variant.getReservedQuantity() < 0) {
                variant.setReservedQuantity(0);
            }
            productVariantRepository.save(variant);
        }
    }

    private String generateOrderNumber() {
        ZoneId zone = ZoneId.of("Asia/Thimphu");
        String date = LocalDate.now(zone).format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < 8; attempt++) {
            int seq = ThreadLocalRandom.current().nextInt(1, 10_000);
            String candidate = String.format("BAH-%s-%04d", date, seq);
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate unique order number");
    }

    private OrderResponse mapOrderToResponse(Order order, boolean includeCustomerDetails, String stripeClientSecret) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> {
                    var variant = item.getVariant();
                    var product = variant.getProduct();
                    String imageUrl = null;
                    if (variant.getImages() != null && !variant.getImages().isEmpty()) {
                        imageUrl = variant.getImages().stream()
                                .filter(img -> Boolean.TRUE.equals(img.isPrimary()))
                                .findFirst()
                                .orElse(variant.getImages().get(0))
                                .getImageUrl();
                    } else if (variant.getGroup() != null
                            && variant.getGroup().getImages() != null
                            && !variant.getGroup().getImages().isEmpty()) {
                        imageUrl = variant.getGroup().getImages().stream()
                                .filter(img -> Boolean.TRUE.equals(img.isPrimary()))
                                .findFirst()
                                .orElse(variant.getGroup().getImages().iterator().next())
                                .getImageUrl();
                    }
                    return OrderItemResponse.builder()
                            .id(item.getId())
                            .variantId(variant.getId())
                            .productSlug(product.getSlug())
                            .productName(item.getProductName())
                            .imageUrl(imageUrl)
                            .sku(item.getSku())
                            .size(item.getSize())
                            .color(item.getColor())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .totalPrice(item.getTotalPrice())
                            .build();
                })
                .toList();

        OrderCustomerSummary customer = null;
        ShippingAddressSummary shippingAddress = null;
        if (includeCustomerDetails) {
            User user = order.getUser();
            customer = OrderCustomerSummary.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phoneNumber(user.getPhoneNumber())
                    .build();
        }
        if (order.getShippingAddress() != null) {
            var addr = order.getShippingAddress();
            shippingAddress = ShippingAddressSummary.builder()
                    .streetAddress(addr.getStreetAddress())
                    .city(addr.getCity())
                    .state(addr.getState())
                    .postalCode(addr.getPostalCode())
                    .country(addr.getCountry())
                    .build();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .tax(order.getTax())
                .shippingCost(order.getShippingCost())
                .total(order.getTotal())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .couponCode(order.getCouponCode())
                .notes(order.getNotes())
                .referralType(order.getReferralType())
                .referralCode(order.getReferralCode())
                .referralPartnerDisplayName(order.getReferralPartnerDisplayName())
                .exchangeRateUsed(order.getExchangeRateUsed())
                .stripePaymentIntentId(order.getStripePaymentIntentId())
                .chargedCurrency(order.getChargedCurrency())
                .trackingNumber(order.getTrackingNumber())
                .deliveredAt(order.getDeliveredAt())
                .stripeClientSecret(stripeClientSecret)
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .customer(customer)
                .shippingAddress(shippingAddress)
                .build();
    }
}
