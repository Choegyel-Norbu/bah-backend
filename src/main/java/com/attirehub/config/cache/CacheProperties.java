package com.attirehub.config.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Externalized cache configuration bound to {@code app.cache.*} in application.yml.
 * Adding a new cache requires only a one-line TTL entry here and a constant in {@link CacheNames}.
 */
@Data
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    private boolean enabled = true;

    /**
     * When true, log cache HIT/MISS at DEBUG for every get and PUT for every put.
     * Enable temporarily to verify data is served from Redis (e.g. app.cache.log-hits-and-misses=true).
     */
    private boolean logHitsAndMisses = false;

    private String keyPrefix = "attirehub:";

    private Duration defaultTtl = Duration.ofMinutes(15);

    /**
     * Per-cache TTL overrides. Keys must match constants in {@link CacheNames}.
     * Example in YAML: {@code app.cache.ttls.product-detail: 15m}
     */
    private Map<String, Duration> ttls = new HashMap<>();

    public Duration getTtlFor(String cacheName) {
        return ttls.getOrDefault(cacheName, defaultTtl);
    }
}
