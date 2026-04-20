-- Add variant_id to reviews for variant-level reviews.
ALTER TABLE reviews
    ADD COLUMN variant_id BIGINT NULL AFTER product_id,
    ADD CONSTRAINT fk_review_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE;

CREATE INDEX idx_reviews_variant_id ON reviews (variant_id);

