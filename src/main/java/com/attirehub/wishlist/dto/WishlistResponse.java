package com.attirehub.wishlist.dto;

import com.attirehub.product.dto.ProductDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse {

    private Long id;
    private ProductDetailResponse product;
    private LocalDateTime addedAt;
}
