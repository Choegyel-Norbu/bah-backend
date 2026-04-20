package com.attirehub.config.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;

/**
 * Cache decorator that logs HIT/MISS on get and PUT on put at DEBUG level.
 * Used when {@code app.cache.log-hits-and-misses=true} to verify data is served from Redis.
 */
final class LoggingCacheDecorator implements Cache {

    private static final Logger log = LoggerFactory.getLogger(LoggingCacheDecorator.class);

    private final Cache delegate;

    LoggingCacheDecorator(Cache delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    @Nullable
    public ValueWrapper get(Object key) {
        ValueWrapper value = delegate.get(key);
        if (value != null) {
            log.debug("CACHE HIT  [{}] key={}", getName(), key);
        } else {
            log.debug("CACHE MISS [{}] key={}", getName(), key);
        }
        return value;
    }

    @Override
    public <T> T get(Object key, @Nullable Class<T> type) {
        T value = delegate.get(key, type);
        if (value != null) {
            log.debug("CACHE HIT  [{}] key={}", getName(), key);
        } else {
            log.debug("CACHE MISS [{}] key={}", getName(), key);
        }
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return delegate.get(key, valueLoader);
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        delegate.put(key, value);
        log.debug("CACHE PUT  [{}] key={}", getName(), key);
    }

    @Override
    public void evict(Object key) {
        delegate.evict(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }
}
