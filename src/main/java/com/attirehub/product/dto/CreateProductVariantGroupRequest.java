package com.attirehub.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductVariantGroupRequest {

    @NotBlank(message = "Color is required")
    @Size(max = 50)
    private String color;

    @Builder.Default
    private boolean isActive = true;

    @Valid
    @Builder.Default
    private List<CreateVariantSizeOptionRequest> sizes = new ArrayList<>();
}

