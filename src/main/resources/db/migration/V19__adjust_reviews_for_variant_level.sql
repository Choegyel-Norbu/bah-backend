-- Adjust reviews table for variant-level reviews.

-- 1) Drop old unique constraint on (user_id, product_id) if it exists.
ALTER TABLE reviews
    DROP INDEX uk_review_user_product;

-- 2) Make product_id nullable so new variant-level reviews don't have to populate it.
ALTER TABLE reviews
    MODIFY COLUMN product_id BIGINT NULL;

-- 3) Add unique constraint on (user_id, variant_id) to enforce one review per user per variant.
ALTER TABLE reviews
    ADD CONSTRAINT uk_review_user_variant UNIQUE (user_id, variant_id);

