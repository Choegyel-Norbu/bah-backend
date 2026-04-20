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
}
