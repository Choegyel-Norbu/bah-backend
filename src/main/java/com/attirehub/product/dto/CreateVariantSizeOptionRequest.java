package com.attirehub.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVariantSizeOptionRequest {

    /**
     * Optional for updates: existing size-option (product_variant) id.
     * When present, backend updates that row instead of inserting a new one.
     */
    private Long id;

    /** Optional. When blank or null, backend generates from product slug + size + color. */
    @Size(max = 100)
    private String sku;

    @NotBlank(message = "Size is required")
    @Size(max = 10)
    private String size;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0", inclusive = false, message = "Price must be positive")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Min(0)
    private int stockQuantity;

    @Builder.Default
    private boolean isActive = true;
}

