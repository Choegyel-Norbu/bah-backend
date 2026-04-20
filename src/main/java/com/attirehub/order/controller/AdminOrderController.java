package com.attirehub.order.controller;

import com.attirehub.order.dto.OrderResponse;
import com.attirehub.order.dto.UpdateOrderStatusRequest;
import com.attirehub.order.service.OrderService;
import com.attirehub.shared.dto.ApiResponse;
import com.attirehub.shared.dto.PagedResponse;
import com.attirehub.shared.enums.OrderStatus;
import com.attirehub.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only order endpoints. Secured by /api/v1/admin/** in SecurityConfig and @PreAuthorize.
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status) {
        PagedResponse<OrderResponse> orders = orderService.getAllOrders(page, size, status);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PutMapping("/{orderNumber}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String orderNumber,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse order = orderService.updateOrderStatus(currentUser.getId(), orderNumber, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }
}
