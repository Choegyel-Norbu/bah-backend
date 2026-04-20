package com.attirehub.coupon.service;

import com.attirehub.coupon.dto.CouponValidationResponse;
import com.attirehub.coupon.entity.Coupon;
import com.attirehub.coupon.repository.CouponRepository;
import com.attirehub.shared.enums.DiscountType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, BigDecimal orderAmount) {
        var couponOpt = couponRepository.findByCode(code.toUpperCase().trim());

        if (couponOpt.isEmpty()) {
            return invalidResponse("Coupon not found");
        }

        Coupon coupon = couponOpt.get();

        // Check active
        if (!coupon.isActive()) {
            return invalidResponse("Coupon is no longer active");
        }

        // Check validity dates
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            return invalidResponse("Coupon has expired or is not yet valid");
        }

        // Check usage limit
        if (coupon.getUsageLimit() != null && coupon.getTimesUsed() >= coupon.getUsageLimit()) {
            return invalidResponse("Coupon usage limit has been reached");
        }

        // Check minimum order amount
        if (coupon.getMinimumOrderAmount() != null
                && orderAmount.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            return invalidResponse(String.format("Minimum order amount of %s required",
                    coupon.getMinimumOrderAmount()));
        }

        // Calculate discount
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            // Cap at max discount if set
            if (coupon.getMaxDiscountAmount() != null
                    && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        } else {
            discount = coupon.getDiscountValue();
        }

        // Discount cannot exceed order amount
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return CouponValidationResponse.builder()
                .valid(true)
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .calculatedDiscount(discount)
                .message("Coupon applied successfully")
                .build();
    }

    private CouponValidationResponse invalidResponse(String message) {
        return CouponValidationResponse.builder()
                .valid(false)
                .message(message)
                .build();
    }
}
