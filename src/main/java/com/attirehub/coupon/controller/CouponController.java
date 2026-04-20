package com.attirehub.coupon.controller;

import com.attirehub.coupon.dto.CouponValidationResponse;
import com.attirehub.coupon.service.CouponService;
import com.attirehub.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal orderAmount) {
        CouponValidationResponse response = couponService.validateCoupon(code, orderAmount);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
