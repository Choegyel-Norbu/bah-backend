package com.attirehub.coupon.service;

import com.attirehub.coupon.dto.CouponValidationResponse;

import java.math.BigDecimal;

public interface CouponService {

    CouponValidationResponse validateCoupon(String code, BigDecimal orderAmount);
}
