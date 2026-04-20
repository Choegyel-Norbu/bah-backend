package com.attirehub.config.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * Wraps a delegate CacheManager and returns cache instances that log HIT/MISS and PUT
 * when {@link CacheProperties#isLogHitsAndMisses()} is true.
 */
final class LoggingCacheManagerDecorator implements CacheManager {

    private final CacheManager delegate;
    private final boolean logHitsAndMisses;

    LoggingCacheManagerDecorator(CacheManager delegate, boolean logHitsAndMisses) {
        this.delegate = delegate;
        this.logHitsAndMisses = logHitsAndMisses;
    }

    @Override
    @Nullable
    public Cache getCache(String name) {
        Cache cache = delegate.getCache(name);
        if (cache == null) {
            return null;
        }
        if (logHitsAndMisses) {
            return new LoggingCacheDecorator(cache);
        }
        return cache;
    }

    @Override
    public Collection<String> getCacheNames() {
        Collection<String> names = delegate.getCacheNames();
        return names != null ? names : Collections.emptyList();
    }
}
