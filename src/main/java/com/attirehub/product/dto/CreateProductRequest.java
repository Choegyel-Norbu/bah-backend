package com.attirehub.product.dto;

import com.attirehub.shared.enums.SourcingType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for admin creating a product (with optional variants).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    private String name;

    /** URL-friendly slug. If blank, generated from name. */
    @Size(max = 255)
    private String slug;

    @Size(max = 65535)
    private String description;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @Size(max = 100)
    private String brand;

    @Size(max = 100)
    private String material;

    /**
     * OWNED vs CONSIGNMENT. If omitted, defaults to OWNED.
     */
    private SourcingType sourcingType;

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private boolean isFeatured = false;

    @Builder.Default
    private boolean isNewArrival = false;

    @Builder.Default
    private boolean isTrending = false;

    @Valid
    @Builder.Default
    private List<CreateProductVariantRequest> variants = new ArrayList<>();

    /**
     * Preferred: color variant groups with size options.
     * If provided (non-empty), backend uses this and ignores {@link #variants}.
     */
    @Valid
    @Builder.Default
    private List<CreateProductVariantGroupRequest> variantGroups = new ArrayList<>();
}
