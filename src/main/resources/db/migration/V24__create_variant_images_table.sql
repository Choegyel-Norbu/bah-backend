-- Create table for variant images (1:N from product_variants to variant_images)
CREATE TABLE variant_images (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    variant_id  BIGINT          NOT NULL,
    image_url   VARCHAR(500)    NOT NULL,
    is_primary  BOOLEAN         NOT NULL DEFAULT FALSE,
    sort_order  INT             NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_variant_images_variant FOREIGN KEY (variant_id)
        REFERENCES product_variants (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_variant_images_variant_id ON variant_images (variant_id);
CREATE INDEX idx_variant_images_sort_order ON variant_images (variant_id, sort_order);

