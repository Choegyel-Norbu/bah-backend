package com.attirehub.wishlist.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistAddRequest {

    @NotNull(message = "Product id is required")
    @Positive(message = "Product id must be a positive number")
    private Long productId;
}
