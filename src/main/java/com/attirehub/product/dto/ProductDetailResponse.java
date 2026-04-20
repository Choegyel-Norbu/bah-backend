package com.attirehub.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full product detail DTO including all variants.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String categoryName;
    private String categorySlug;
    private String brand;
    private String material;
    private boolean isFeatured;
    private boolean isTrending;
    private boolean newArrival;
    private BigDecimal averageRating;
    private int reviewCount;
    /**
     * Discount amount (in currency) for the primary on-sale variant for this product.
     * Null when the product is not on sale.
     */
    private BigDecimal discount;
    private List<VariantGroupResponse> variantGroups;
}
