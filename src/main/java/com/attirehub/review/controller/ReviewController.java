package com.attirehub.review.controller;

import com.attirehub.review.dto.CreateReviewRequest;
import com.attirehub.review.dto.ReviewResponse;
import com.attirehub.review.dto.UpdateReviewRequest;
import com.attirehub.review.service.ReviewService;
import com.attirehub.shared.dto.ApiResponse;
import com.attirehub.shared.dto.PagedResponse;
import com.attirehub.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Product reviews. Only users who have purchased the product (verified purchase) may create a review.
 * GET is public; POST, PUT, DELETE require authentication.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/{productId}/reviews")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getReviews(
            @PathVariable Long productId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PagedResponse<ReviewResponse> result = (variantId != null)
                ? reviewService.getReviewsByProductAndVariant(productId, variantId, pageable)
                : reviewService.getReviewsByProductId(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{productId}/reviews/me")
    public ResponseEntity<ApiResponse<ReviewResponse>> getMyReview(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long productId,
            @RequestParam Long variantId) {
        ReviewResponse review = reviewService.getMyReview(productId, variantId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(review));
    }

    @PostMapping("/{productId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long productId,
            @RequestParam Long variantId,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse review = reviewService.create(productId, variantId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted", review));
    }

    @PutMapping("/{productId}/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {
        ReviewResponse review = reviewService.update(productId, reviewId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Review updated", review));
    }

    @DeleteMapping("/{productId}/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long productId,
            @PathVariable Long reviewId) {
        reviewService.delete(productId, reviewId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Review deleted"));
    }
}
