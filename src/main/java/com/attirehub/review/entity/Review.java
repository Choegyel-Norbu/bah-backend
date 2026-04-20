package com.attirehub.review.entity;

import com.attirehub.product.entity.ProductVariant;
import com.attirehub.shared.entity.BaseEntity;
import com.attirehub.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * Product variant review. Only users who have purchased the variant (verified purchase) may create a review.
 * One review per user per variant; user may update or delete their own review.
 */
@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_user_variant", columnNames = {"user_id", "variant_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int rating;

    @Column(length = 2000)
    private String comment;

    // verifiedPurchase is not stored: it is true for all reviews (only purchasers can create)
}
