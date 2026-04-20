package com.attirehub.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantGroupResponse {

    private Long id;
    private String color;
    private boolean isActive;
    private List<VariantGroupImageResponse> images;

    /**
     * Size-specific purchasable options under this color group.
     */
    private List<VariantResponse> sizeOptions;
}

