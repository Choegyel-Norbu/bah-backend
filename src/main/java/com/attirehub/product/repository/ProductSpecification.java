package com.attirehub.product.repository;

import com.attirehub.product.entity.Product;
import com.attirehub.shared.enums.ProductStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * JPA Specification builder for dynamic product filtering.
 * Supports filtering by category, size, color, price range, search term, and featured status.
 */
public final class ProductSpecification {

    private ProductSpecification() {}

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.equal(root.get("productStatus"), ProductStatus.ACTIVE);
    }

    public static Specification<Product> hasCategory(String categorySlug) {
        return (root, query, cb) -> {
            root.join("category", JoinType.LEFT);
            return cb.equal(root.get("category").get("slug"), categorySlug);
        };
    }

    /** Product belongs to any of the given category IDs (e.g. parent + its children). */
    public static Specification<Product> hasCategoryIn(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return (root, query, cb) -> root.join("category", JoinType.INNER).get("id").in(Collections.emptyList());
        }
        return (root, query, cb) -> {
            var category = root.join("category", JoinType.INNER);
            return category.get("id").in(categoryIds);
        };
    }

    public static Specification<Product> hasSize(String size) {
        return (root, query, cb) -> {
            var variants = root.join("variants", JoinType.INNER);
            return cb.equal(variants.get("size"), size);
        };
    }

    public static Specification<Product> hasColor(String color) {
        return (root, query, cb) -> {
            var variants = root.join("variants", JoinType.INNER);
            return cb.equal(cb.lower(variants.get("color")), color.toLowerCase());
        };
    }

    /** Product has at least one active variant with price >= minPrice. */
    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) -> {
            var variants = root.join("variants", JoinType.INNER);
            query.distinct(true);
            return cb.greaterThanOrEqualTo(variants.get("price"), minPrice);
        };
    }

    /** Product has at least one active variant with price <= maxPrice. */
    public static Specification<Product> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) -> {
            var variants = root.join("variants", JoinType.INNER);
            query.distinct(true);
            return cb.lessThanOrEqualTo(variants.get("price"), maxPrice);
        };
    }

    public static Specification<Product> isFeatured() {
        return (root, query, cb) -> cb.isTrue(root.get("isFeatured"));
    }

    public static Specification<Product> isTrending() {
        return (root, query, cb) -> cb.isTrue(root.get("isTrending"));
    }

    /** Product is marked as new arrival (admin-set flag). */
    public static Specification<Product> isNewArrival() {
        return (root, query, cb) -> cb.isTrue(root.get("isNewArrival"));
    }

    /** Product has at least one variant with discount > 0 (on sale). */
    public static Specification<Product> onSale() {
        return (root, query, cb) -> {
            var variants = root.join("variants", JoinType.INNER);
            query.distinct(true);
            return cb.greaterThan(variants.get("discount"), BigDecimal.ZERO);
        };
    }

    public static Specification<Product> searchByName(String search) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
    }

}
