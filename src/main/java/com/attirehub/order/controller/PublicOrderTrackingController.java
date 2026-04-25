package com.attirehub.order.controller;

import com.attirehub.order.dto.OrderResponse;
import com.attirehub.order.service.OrderService;
import com.attirehub.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PRD CX-07 — order lookup without login (order number + email).
 */
@RestController
@RequestMapping("/api/v1/public/orders")
@RequiredArgsConstructor
public class PublicOrderTrackingController {

    private final OrderService orderService;

    @GetMapping("/track")
    public ResponseEntity<ApiResponse<OrderResponse>> track(
            @RequestParam String orderNumber,
            @RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(orderService.trackOrder(orderNumber, email)));
    }
}
