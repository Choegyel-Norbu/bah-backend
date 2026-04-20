package com.attirehub.review.service;

import com.attirehub.order.repository.OrderItemRepository;
import com.attirehub.product.repository.ProductRepository;
import com.attirehub.product.entity.ProductVariant;
import com.attirehub.product.repository.ProductVariantRepository;
import com.attirehub.review.dto.CreateReviewRequest;
import com.attirehub.review.dto.ReviewResponse;
import com.attirehub.review.dto.UpdateReviewRequest;
import com.attirehub.review.entity.Review;
import com.attirehub.review.mapper.ReviewMapper;
import com.attirehub.review.repository.ReviewRepository;
import com.attirehub.shared.dto.PagedResponse;
import com.attirehub.shared.exception.BadRequestException;
import com.attirehub.shared.exception.ResourceNotFoundException;
import com.attirehub.user.entity.User;
import com.attirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getReviewsByProductId(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }
        Page<Review> page = reviewRepository.findByVariantProductIdOrderByCreatedAtDesc(productId, pageable);
        List<ReviewResponse> content = page.getContent().stream()
                .map(reviewMapper::toResponse)
                .toList();
        return PagedResponse.<ReviewResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional
    public PagedResponse<ReviewResponse> getReviewsByProductAndVariant(Long productId, Long variantId, Pageable pageable) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant", "id", variantId));
        if (!variant.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Variant does not belong to this product.");
        }

        Page<Review> page = reviewRepository.findByVariantIdOrderByCreatedAtDesc(variantId, pageable);
        List<ReviewResponse> content = page.getContent().stream()
                .map(reviewMapper::toResponse)
                .toList();
        return PagedResponse.<ReviewResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional
    public ReviewResponse create(Long productId, Long variantId, Long userId, CreateReviewRequest request) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant", "id", variantId));
        if (!variant.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Variant does not belong to this product.");
        }

        if (!orderItemRepository.hasUserPurchasedVariant(userId, variantId)) {
            throw new BadRequestException("You can only review variants you have purchased (order must be delivered or shipped).");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Review review;
        var existing = reviewRepository.findByUserIdAndVariantId(userId, variantId);
        if (existing.isPresent()) {
            review = existing.get();
            review.setRating(request.getRating());
            review.setComment(request.getComment() != null ? request.getComment().trim() : null);
            review = reviewRepository.save(review);
            log.info("Review updated: reviewId={}, productId={}, userId={}", review.getId(), productId, userId);
        } else {
            review = Review.builder()
                    .variant(variant)
                    .user(user)
                    .rating(request.getRating())
                    .comment(request.getComment() != null ? request.getComment().trim() : null)
                    .build();
            review = reviewRepository.save(review);
            log.info("Review created: reviewId={}, productId={}, variantId={}, userId={}",
                    review.getId(), productId, variantId, userId);
        }

        updateProductRatingAggregates(productId);
        return reviewMapper.toResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse update(Long productId, Long reviewId, Long userId, UpdateReviewRequest request) {
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        if (!review.getVariant().getProduct().getId().equals(productId)) {
            throw new BadRequestException("Review does not belong to this product.");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment() != null ? request.getComment().trim() : null);
        review = reviewRepository.save(review);
        updateProductRatingAggregates(productId);
        log.info("Review updated: reviewId={}, productId={}, userId={}", reviewId, productId, userId);
        return reviewMapper.toResponse(review);
    }

    @Override
    @Transactional
    public void delete(Long productId, Long reviewId, Long userId) {
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        if (!review.getVariant().getProduct().getId().equals(productId)) {
            throw new BadRequestException("Review does not belong to this product.");
        }
        reviewRepository.delete(review);
        updateProductRatingAggregates(productId);
        log.info("Review deleted: reviewId={}, productId={}, userId={}", reviewId, productId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getMyReview(Long productId, Long variantId, Long userId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant", "id", variantId));
        if (!variant.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Variant does not belong to this product.");
        }

        return reviewRepository.findByUserIdAndVariantId(userId, variantId)
                .map(reviewMapper::toResponse)
                .orElse(null);
    }

    private void updateProductRatingAggregates(Long productId) {
        var product = productRepository.findById(productId).orElse(null);
        if (product == null) return;

        long count = reviewRepository.countByProductId(productId);
        if (count == 0) {
            product.setAverageRating(null);
            product.setReviewCount(0);
        } else {
            double avg = reviewRepository.getAverageRatingByProductId(productId);
            product.setAverageRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            product.setReviewCount((int) count);
        }
        productRepository.save(product);
    }
}
