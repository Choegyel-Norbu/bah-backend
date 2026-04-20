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
import com.attirehub.product.entity.ProductVariant;
import com.attirehub.product.repository.ProductVariantRepository;
import com.attirehub.shared.dto.PagedResponse;
import com.attirehub.shared.enums.OrderStatus;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository productVariantRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        // 1. Get user's cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        // 2. Validate shipping address belongs to user
        Address shippingAddress = addressRepository.findByIdAndUserId(request.getShippingAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", request.getShippingAddressId()));

        // 3. Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 4. Build order
        String orderNumber = generateOrderNumber();
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .shippingAddress(shippingAddress)
                .couponCode(request.getCouponCode())
                .notes(request.getNotes())
                .build();

        // 5. Process each cart item → order item + deduct stock
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = cartItem.getVariant();

            // Validate stock availability
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException(
                        String.format("Insufficient stock for %s (%s/%s). Available: %d, Requested: %d",
                                variant.getProduct().getName(), variant.getSize(), variant.getColor(),
                                variant.getStockQuantity(), cartItem.getQuantity()));
            }

            // Deduct stock (optimistic locking via @Version)
            variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
            productVariantRepository.save(variant);

            // Use variant discount from DB (flat amount) when creating order item totals.
            BigDecimal variantDiscount = variant.getDiscount() != null ? variant.getDiscount() : BigDecimal.ZERO;
            BigDecimal discountedUnitPrice = variant.getPrice().subtract(variantDiscount);
            if (discountedUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
                discountedUnitPrice = BigDecimal.ZERO;
            }
            discountedUnitPrice = discountedUnitPrice.setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemTotal = discountedUnitPrice
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variant(variant)
                    .productName(variant.getProduct().getName())
                    .sku(variant.getSku())
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(discountedUnitPrice)
                    .totalPrice(itemTotal)
                    .build();

            order.getItems().add(orderItem);
            subtotal = subtotal.add(itemTotal);
        }

        // 6. Calculate totals
        order.setSubtotal(subtotal);
        order.setTotal(subtotal.subtract(order.getDiscount())
                .add(order.getTax())
                .add(order.getShippingCost()));

        // 7. Add initial status history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(OrderStatus.PENDING)
                .notes("Order placed")
                .changedBy(userId)
                .build();
        order.getStatusHistory().add(history);

        // 8. Save order
        Order savedOrder = orderRepository.save(order);

        // 9. Store notification for new order (notification module)
        String totalMessage = "Total: " + savedOrder.getTotal();
        notificationService.createForNewOrder(savedOrder.getId(), userId, orderNumber, totalMessage);

        // 10. Clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Order placed: orderNumber={}, userId={}, total={}", orderNumber, userId, order.getTotal());
        return mapOrderToResponse(savedOrder, false);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, String orderNumber) {
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return mapOrderToResponse(order, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getUserOrders(Long userId, int page, int size, OrderStatus statusFilter) {
        Page<Order> orderPage = statusFilter != null
                ? orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, statusFilter, PageRequest.of(page, size))
                : orderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        List<OrderResponse> content = orderPage.getContent()
                .stream()
                .map(order -> mapOrderToResponse(order, false))
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

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Order can only be cancelled when in PENDING or CONFIRMED status");
        }

        // Restore stock
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            productVariantRepository.save(variant);
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
        return mapOrderToResponse(savedOrder, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(int page, int size, OrderStatus statusFilter) {
        Page<Order> orderPage = statusFilter != null
                ? orderRepository.findAllByStatusOrderByCreatedAtDesc(statusFilter, PageRequest.of(page, size))
                : orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        List<OrderResponse> content = orderPage.getContent().stream()
                .map(order -> mapOrderToResponse(order, true))
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
        return mapOrderToResponse(saved, true);
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = ThreadLocalRandom.current().nextInt(100, 1000);
        return String.format("ORD-%s-%03d", timestamp, random);
    }

    private OrderResponse mapOrderToResponse(Order order, boolean includeCustomerDetails) {
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
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .customer(customer)
                .shippingAddress(shippingAddress)
                .build();
    }
}
