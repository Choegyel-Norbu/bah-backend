package com.attirehub.order.service;

import com.attirehub.order.dto.OrderResponse;
import com.attirehub.order.dto.PlaceOrderRequest;
import com.attirehub.order.dto.UpdateOrderStatusRequest;
import com.attirehub.shared.dto.PagedResponse;
import com.attirehub.shared.enums.OrderStatus;

public interface OrderService {

    OrderResponse placeOrder(Long userId, PlaceOrderRequest request);

    OrderResponse getOrder(Long userId, String orderNumber);

    PagedResponse<OrderResponse> getUserOrders(Long userId, int page, int size, OrderStatus statusFilter);

    OrderResponse cancelOrder(Long userId, String orderNumber);

    /** Admin: list all orders, newest first; optional status filter. */
    PagedResponse<OrderResponse> getAllOrders(int page, int size, OrderStatus statusFilter);

    /** Admin: update order status and record in history. */
    OrderResponse updateOrderStatus(Long adminUserId, String orderNumber, UpdateOrderStatusRequest request);

    /** Scheduled: release stock for Stripe checkouts that never completed payment (PRD ~30 min). */
    void expireStalePendingPaymentOrders();

    /** Scheduled: DELIVERED → COMPLETED after cooling-off period (PRD: 14 days). */
    void autoCompleteDeliveredOrders();

    /** Public tracking (PRD CX-07): order number + email must match the order's customer. */
    OrderResponse trackOrder(String orderNumber, String email);
}
