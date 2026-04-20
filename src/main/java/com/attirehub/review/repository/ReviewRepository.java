package com.attirehub.review.repository;

import com.attirehub.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.variant.product.id = :productId")
    double getAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.variant.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);

    @EntityGraph(attributePaths = {"user", "variant", "variant.product"})
    Page<Review> findByVariantProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "variant", "variant.product"})
    Page<Review> findByVariantIdOrderByCreatedAtDesc(Long variantId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.id = :id AND r.user.id = :userId")
    Optional<Review> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Optional<Review> findByUserIdAndVariantId(Long userId, Long variantId);

    boolean existsByUserIdAndVariantId(Long userId, Long variantId);
}
