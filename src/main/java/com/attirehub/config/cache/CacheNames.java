package com.attirehub.config.cache;

import java.util.List;

/**
 * Central registry of all cache names used in the application.
 * Each constant maps to a named Redis cache with its own TTL
 * configured in {@link CacheProperties}.
 *
 * <p>Key format: {@code attirehub:<cacheName>::<key>}
 */
public final class CacheNames {

    public static final String PRODUCT_DETAIL = "product-detail";
    public static final String PRODUCT_VARIANTS = "product-variants";
    public static final String PRODUCT_RELATED = "product-related";
    public static final String PRODUCT_TRENDING = "product-trending";
    public static final String CATEGORIES = "categories";

    private CacheNames() {
    }

    public static List<String> all() {
        return List.of(
                PRODUCT_DETAIL,
                PRODUCT_VARIANTS,
                PRODUCT_RELATED,
                PRODUCT_TRENDING,
                CATEGORIES
        );
    }
}
