package com.attirehub.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating a product variant (admin).
 * All fields optional: only non-null values are applied.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductVariantRequest {

    @Size(max = 100)
    private String sku;

    @Size(max = 10)
    private String size;

    @Size(max = 50)
    private String color;

    @DecimalMin(value = "0", inclusive = false, message = "Price must be positive")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    private BigDecimal discount;

    @Min(0)
    private Integer stockQuantity;

    @Size(max = 500)
    private String imageUrl;

    private Boolean isActive;
}
