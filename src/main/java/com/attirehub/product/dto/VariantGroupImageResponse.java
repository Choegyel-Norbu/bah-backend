package com.attirehub.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantGroupImageResponse {

    private Long id;
    private String imageUrl;
    private boolean primary;
    private int sortOrder;
}

