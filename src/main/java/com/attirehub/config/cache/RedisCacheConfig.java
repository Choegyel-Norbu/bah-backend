package com.attirehub.config.cache;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@RequiredArgsConstructor
public class RedisCacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    private final CacheProperties cacheProperties;
    private final RedisConnectionFactory redisConnectionFactory;

    @Bean
    @Override
    public CacheManager cacheManager() {
        if (!cacheProperties.isEnabled()) {
            log.info("Cache disabled (app.cache.enabled=false) — using NoOpCacheManager");
            return new NoOpCacheManager();
        }

        GenericJackson2JsonRedisSerializer serializer = createJsonSerializer();
        RedisCacheConfiguration defaultConfig = buildCacheConfig(cacheProperties.getDefaultTtl(), serializer);

        Map<String, RedisCacheConfiguration> perCacheConfigs = new HashMap<>();
        for (String cacheName : CacheNames.all()) {
            Duration ttl = cacheProperties.getTtlFor(cacheName);
            perCacheConfigs.put(cacheName, buildCacheConfig(ttl, serializer));
        }

        RedisCacheManager manager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfigs)
                .transactionAware()
                .build();

        log.info("Redis CacheManager initialized — prefix='{}', caches={}, logHitsAndMisses={}",
                cacheProperties.getKeyPrefix(), perCacheConfigs.keySet(), cacheProperties.isLogHitsAndMisses());

        if (cacheProperties.isLogHitsAndMisses()) {
            return new LoggingCacheManagerDecorator(manager, true);
        }
        return manager;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new GracefulCacheErrorHandler();
    }

    private RedisCacheConfiguration buildCacheConfig(Duration ttl, GenericJackson2JsonRedisSerializer serializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(cacheProperties.getKeyPrefix())
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer));
    }

    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.attirehub.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.math.")
                        .allowIfSubType("java.time.")
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    /**
     * Logs cache errors instead of propagating them so the application
     * continues to function when Redis is temporarily unavailable.
     */
    static class GracefulCacheErrorHandler implements CacheErrorHandler {

        private static final Logger log = LoggerFactory.getLogger(GracefulCacheErrorHandler.class);

        @Override
        public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
            log.warn("Cache GET failed [cache={}, key={}]: {}", cache.getName(), key, ex.getMessage());
        }

        @Override
        public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
            log.warn("Cache PUT failed [cache={}, key={}]: {}", cache.getName(), key, ex.getMessage());
        }

        @Override
        public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
            log.warn("Cache EVICT failed [cache={}, key={}]: {}", cache.getName(), key, ex.getMessage());
        }

        @Override
        public void handleCacheClearError(RuntimeException ex, Cache cache) {
            log.warn("Cache CLEAR failed [cache={}]: {}", cache.getName(), ex.getMessage());
        }
    }
}
