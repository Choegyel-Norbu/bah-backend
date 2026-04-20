package com.attirehub.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product DTO for listing pages; includes active variants for each product.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductListResponse {

    private Long id;
    private String name;
    private String slug;
    private String categoryName;
    private String categorySlug;
    private String brand;
    private boolean isFeatured;
    private boolean isTrending;
    /** True if product is marked as new arrival (admin-set). */
    private boolean newArrival;
    private BigDecimal averageRating;
    private int reviewCount;
    /**
     * Discount amount (in currency) for the primary on-sale variant used in listings.
     * Null when the product is not on sale.
     */
    private BigDecimal discount;
    private List<VariantGroupResponse> variantGroups;
}
