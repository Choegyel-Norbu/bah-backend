package com.attirehub.order.repository;

import com.attirehub.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Returns product IDs ranked by units sold in completed orders within the given time window.
     * Only counts DELIVERED and SHIPPED orders.
     */
    @Query("""
            SELECT oi.variant.product.id
            FROM OrderItem oi
            WHERE oi.order.status IN ('DELIVERED', 'SHIPPED')
              AND oi.order.createdAt >= :since
            GROUP BY oi.variant.product.id
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Long> findTrendingProductIds(@Param("since") LocalDateTime since, org.springframework.data.domain.Pageable pageable);

    /**
     * Returns whether the user has purchased the given product (at least one order item
     * in a DELIVERED or SHIPPED order). Used to enforce verified-purchase for reviews.
     */
    @Query("""
            SELECT COUNT(oi) > 0
            FROM OrderItem oi
            WHERE oi.order.user.id = :userId
              AND oi.variant.product.id = :productId
              AND oi.order.status IN ('DELIVERED', 'SHIPPED')
            """)
    boolean hasUserPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * Returns whether the user has purchased the given variant (at least one order item
     * in a DELIVERED or SHIPPED order). Used to enforce verified-purchase for variant-level reviews.
     */
    @Query("""
            SELECT COUNT(oi) > 0
            FROM OrderItem oi
            WHERE oi.order.user.id = :userId
              AND oi.variant.id = :variantId
              AND oi.order.status IN ('DELIVERED', 'SHIPPED')
            """)
    boolean hasUserPurchasedVariant(@Param("userId") Long userId, @Param("variantId") Long variantId);
}
