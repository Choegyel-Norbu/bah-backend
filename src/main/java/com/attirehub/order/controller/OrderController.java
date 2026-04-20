package com.attirehub.order.controller;

import com.attirehub.order.dto.OrderResponse;
import com.attirehub.order.dto.PlaceOrderRequest;
import com.attirehub.order.service.OrderService;
import com.attirehub.shared.dto.ApiResponse;
import com.attirehub.shared.dto.PagedResponse;
import com.attirehub.shared.enums.OrderStatus;
import com.attirehub.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse order = orderService.placeOrder(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getUserOrders(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status) {
        PagedResponse<OrderResponse> orders =
                orderService.getUserOrders(currentUser.getId(), page, size, status);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String orderNumber) {
        OrderResponse order = orderService.getOrder(currentUser.getId(), orderNumber);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PutMapping("/{orderNumber}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String orderNumber) {
        OrderResponse order = orderService.cancelOrder(currentUser.getId(), orderNumber);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", order));
    }
}
