package com.attirehub.review.service;

import com.attirehub.review.dto.CreateReviewRequest;
import com.attirehub.review.dto.ReviewResponse;
import com.attirehub.review.dto.UpdateReviewRequest;
import com.attirehub.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

/**
 * Review service: only users who have purchased a product (verified purchase) may create a review.
 * One review per user per product; user may update or delete their own review.
 */
public interface ReviewService {

    PagedResponse<ReviewResponse> getReviewsByProductId(Long productId, Pageable pageable);

    PagedResponse<ReviewResponse> getReviewsByProductAndVariant(Long productId, Long variantId, Pageable pageable);

    ReviewResponse create(Long productId, Long variantId, Long userId, CreateReviewRequest request);

    ReviewResponse update(Long productId, Long reviewId, Long userId, UpdateReviewRequest request);

    void delete(Long productId, Long reviewId, Long userId);

    /**
     * Get the current user's review for a specific product variant, if it exists.
     */
    ReviewResponse getMyReview(Long productId, Long variantId, Long userId);
}
