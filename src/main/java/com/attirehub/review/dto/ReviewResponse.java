package com.attirehub.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long productId;
    private Long variantId;
    private Long userId;
    private String userDisplayName;
    private int rating;
    private String comment;
    /** Always true: only purchasers can leave reviews. */
    private boolean verifiedPurchase;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
