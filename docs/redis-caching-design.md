# Redis Caching — Architecture Decision Record

## Status

Accepted

## Context

AttireHub is a B2C e-commerce platform where product catalog browsing (product detail, category listing, trending products) is read-heavy and write-infrequent. Redis caching reduces database load on these high-traffic read paths while maintaining data freshness through targeted eviction on writes.

## Decision

### Cache Provider

- **Redis 7** via Spring Data Redis (Lettuce driver, lazy connections)
- **JSON serialization** with `GenericJackson2JsonRedisSerializer` for human-readable, debuggable cache entries
- **Graceful degradation** — a `CacheErrorHandler` logs and suppresses Redis failures so the app continues to function using the database as fallback

### Cache Names, Keys & TTLs

| Cache Name | Key | TTL | Cached Type |
|---|---|---|---|
| `product-detail` | slug (e.g. `summer-dress`) | 15 min | `ProductDetailResponse` |
| `product-variants` | productId (e.g. `42`) | 15 min | `List<VariantResponse>` |
| `product-related` | productId (e.g. `42`) | 30 min | `List<ProductListResponse>` |
| `product-trending` | limit (e.g. `10`) | 30 min | `List<ProductListResponse>` |
| `categories` | `all` or slug (e.g. `men`) | 1 hour | `List<CategoryResponse>` or `CategoryResponse` |

**Key prefix**: All keys are prefixed with `attirehub:` (configurable via `app.cache.key-prefix`).

**Full Redis key format**: `attirehub:<cache-name>::<key>`
Example: `attirehub:product-detail::summer-dress`

### What Is NOT Cached

| Data | Reason |
|---|---|
| Product list with filters (`getProducts`) | Too many filter combinations → low hit rate, cache pollution |
| Products by category (paginated) | Pagination makes keys complex, moderate benefit |
| Coupon validation | Time-sensitive checks (expiry, usage limits) that must be real-time |
| User cart / wishlist | User-specific, write-heavy |
| Auth tokens (JWT) | Stateless by design |

### Eviction Strategy

Eviction is **programmatic** (via `CacheManager`) rather than annotation-based, because mutation methods receive `productId` while cache keys use `slug`. This gives precise control over which entries are evicted.

| Operation | Caches Evicted |
|---|---|
| `createProduct` | `product-trending` (all entries) |
| `updateProduct` | `product-detail` (old + new slug), `product-variants`, `product-related`, `product-trending` |
| `deleteProduct` | `product-detail`, `product-variants`, `product-related`, `product-trending` |
| `createVariant` / `updateVariant` / `deleteVariant` / `clearVariantImage` | `product-variants`, `product-detail` (by slug) |

Category caches have no admin mutation endpoints yet. When added, evict the `categories` cache on create/update/delete.

### Transaction Awareness

The `RedisCacheManager` is configured with `.transactionAware()`. This means programmatic `cache.evict()` calls inside `@Transactional` methods are **deferred until after the transaction commits**. If the transaction rolls back, evictions are discarded — keeping the cache consistent with the database.

## Configuration

All cache settings are externalized in `application.yml` under `app.cache.*`:

```yaml
app:
  cache:
    enabled: true            # set to false to disable caching entirely (NoOpCacheManager)
    key-prefix: "attirehub:"
    default-ttl: 15m
    ttls:
      product-detail: 15m
      product-variants: 15m
      product-related: 30m
      product-trending: 30m
      categories: 1h
```

### How to Add a New Cache

1. Add a constant in `CacheNames.java` and include it in the `all()` list
2. Add a TTL entry in `application.yml` under `app.cache.ttls`
3. Add `@Cacheable(value = CacheNames.YOUR_CACHE, key = "...")` to the service method
4. Add eviction logic to any mutation methods that affect the cached data

### How to Disable Redis for Local Development

Set `app.cache.enabled: false` in `application.yml` or as an environment variable:

```bash
APP_CACHE_ENABLED=false ./mvnw spring-boot:run
```

The app uses a `NoOpCacheManager` — all `@Cacheable` methods execute normally but nothing is cached. No Redis connection is required.

### Redis Connection

Configured in `application.yml` under `spring.data.redis`:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2000ms
      # password: ${REDIS_PASSWORD:}    # uncomment for production
```

Lettuce (the default Redis driver) uses **lazy connections** — the app starts successfully even if Redis is unavailable. The `GracefulCacheErrorHandler` logs and suppresses runtime Redis errors.

### Redis Password (Production)

Add `spring.data.redis.password` to your production config or environment variables. The auto-configured `LettuceConnectionFactory` uses it automatically.

### Recommended Redis Memory

- **Development**: 64–128 MB (small catalog)
- **Production**: 256–512 MB (scales with catalog size and TTLs)

Monitor with `redis-cli INFO memory` and adjust `maxmemory` + eviction policy (`allkeys-lru` recommended).

## File Structure

```
config/cache/
├── CacheNames.java          # Cache name constants
├── CacheProperties.java     # @ConfigurationProperties for TTLs
└── RedisCacheConfig.java     # CacheManager, serializer, error handler
```

## How to verify products are served from Redis

### 1. Enable cache hit/miss logging (recommended)

Set in `application.yml` or as an environment variable:

```yaml
app:
  cache:
    log-hits-and-misses: true
```

Ensure DEBUG logs are visible for `com.attirehub`:

```yaml
logging:
  level:
    com.attirehub: DEBUG
```

Then call the same product endpoint twice (e.g. `GET /api/products/slug/summer-dress`):

- **First request**: You should see `CACHE MISS [product-detail] key=summer-dress` then the response → data was loaded from the DB and stored in Redis.
- **Second request**: You should see `CACHE HIT [product-detail] key=summer-dress` and no DB query → data was served from Redis.

Leave `log-hits-and-misses: false` in production to avoid log noise.

### 2. Watch Hibernate SQL (no SQL = cache hit)

With `org.hibernate.SQL: DEBUG` (or TRACE) in `application.yml`:

- **First request** for a product by slug: you see `select ... from products ... where slug=?`.
- **Second request** for the same slug: you see **no new SQL** for that product → the response came from the cache.

Same idea for categories: first call to `/api/categories` shows SQL; second call shows no SQL if cached.

### 3. Inspect Redis with redis-cli

With the app running and after at least one product/category request:

```bash
# List all cache keys (pattern: attirehub:<cache-name>::<key>)
redis-cli KEYS 'attirehub:*'

# Example output:
# attirehub:product-detail::summer-dress
# attirehub:categories::all
# attirehub:product-trending::10
```

Watch commands in real time while you hit the API:

```bash
redis-cli MONITOR
```

You’ll see `GET` on cache hits and `SET` (or `get`/`set` depending on client) on first request or after eviction.

---

## Consequences

- **Positive**: Reduced DB load on product reads, sub-millisecond cache hits, graceful degradation
- **Negative**: Brief staleness window (up to TTL) after writes, additional infrastructure dependency
- **Mitigated**: Targeted eviction on mutations keeps staleness window minimal; `GracefulCacheErrorHandler` ensures Redis outages don't break the application
