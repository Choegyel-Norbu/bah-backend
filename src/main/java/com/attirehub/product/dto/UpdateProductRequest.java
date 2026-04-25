package com.attirehub.product.dto;

import com.attirehub.shared.enums.SourcingType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for updating product details (admin).
 * All fields optional: only non-null values are applied.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String slug;

    @Size(max = 65535)
    private String description;

    private Long categoryId;

    @Size(max = 100)
    private String brand;

    @Size(max = 100)
    private String material;

    private SourcingType sourcingType;

    @JsonProperty("active")
    private Boolean isActive;

    @JsonProperty("featured")
    private Boolean isFeatured;

    @JsonProperty("newArrival")
    private Boolean isNewArrival;

    @JsonProperty("trending")
    private Boolean isTrending;

    /**
     * Optional: upsert color variant groups + size options.
     * If provided (non-empty), backend will create missing groups/sizes and update existing ones.
     */
    @Valid
    @Builder.Default
    private List<CreateProductVariantGroupRequest> variantGroups = new ArrayList<>();
}
