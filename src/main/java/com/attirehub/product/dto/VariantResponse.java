package com.attirehub.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantResponse {

    private Long id;
    private String sku;
    private String size;
    private String color;
    private BigDecimal price;
    private BigDecimal discount;
    private int stockQuantity;
    private boolean isActive;

    /**
     * Images associated with this variant, ordered by sortOrder.
     */
    private List<VariantImageResponse> images;
}
