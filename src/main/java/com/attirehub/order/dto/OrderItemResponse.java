package com.attirehub.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private Long id;
    /** Variant id for linking to variant-specific actions (e.g. reviews). */
    private Long variantId;
    /** Product slug for linking to product detail page. */
    private String productSlug;
    private String productName;
    /** Image URL for the variant (or product fallback) for display in order details. */
    private String imageUrl;
    private String sku;
    private String size;
    private String color;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
