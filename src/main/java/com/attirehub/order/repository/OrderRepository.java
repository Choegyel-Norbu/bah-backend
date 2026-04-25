package com.attirehub.order.repository;

import com.attirehub.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.attirehub.shared.enums.OrderStatus;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.variant", "shippingAddress", "user"})
    Optional<Order> findByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "shippingAddress"})
    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "shippingAddress", "user"})
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"items", "shippingAddress", "user"})
    Page<Order> findAllByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    boolean existsByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"items", "items.variant", "user"})
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime createdBefore);

    @EntityGraph(attributePaths = {"items", "user"})
    List<Order> findByStatusAndDeliveredAtBefore(OrderStatus status, LocalDateTime deliveredBefore);

    @Query(value = """
            SELECT DATE_FORMAT(o.created_at, '%Y-%m') AS month_key,
                   COALESCE(SUM(o.total), 0)          AS gross_sales,
                   COUNT(*)                           AS order_count
            FROM orders o
            WHERE o.created_at >= :fromStart
              AND o.created_at < :toExclusive
              AND o.payment_status = 'PAID'
            GROUP BY DATE_FORMAT(o.created_at, '%Y-%m')
            ORDER BY month_key
            """, nativeQuery = true)
    List<Object[]> fetchMonthlySalesTrend(
            @Param("fromStart") LocalDateTime fromStart,
            @Param("toExclusive") LocalDateTime toExclusive);
}
