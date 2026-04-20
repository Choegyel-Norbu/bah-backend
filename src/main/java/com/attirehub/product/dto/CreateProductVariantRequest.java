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

/**
 * Request DTO for creating a product variant (size/color/SKU).
 * SKU is optional: when omitted, the backend generates it from product slug + size + color.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductVariantRequest {

    /** Optional. When blank or null, backend generates from product slug + size + color. */
    @Size(max = 100)
    private String sku;

    @NotBlank(message = "Size is required")
    @Size(max = 10)
    private String size;

    @NotBlank(message = "Color is required")
    @Size(max = 50)
    private String color;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0", inclusive = false, message = "Price must be positive")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    /** Discount percentage. Default 0. */
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Min(0)
    private int stockQuantity;

    @Size(max = 500)
    private String imageUrl;

    @Builder.Default
    private boolean isActive = true;
}
