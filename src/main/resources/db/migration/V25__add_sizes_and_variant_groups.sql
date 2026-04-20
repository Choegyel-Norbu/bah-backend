-- Normalize sizes and add variant grouping (variant can have multiple sizes).

-- 1) Create sizes table
CREATE TABLE sizes (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    label       VARCHAR(10)     NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_sizes_label UNIQUE (label)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) Create variant groups table (grouped by product + color)
CREATE TABLE product_variant_groups (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    product_id  BIGINT          NOT NULL,
    color       VARCHAR(50)     NOT NULL,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_variant_groups_product_color UNIQUE (product_id, color),
    CONSTRAINT fk_variant_groups_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_variant_groups_product_id ON product_variant_groups (product_id);
CREATE INDEX idx_variant_groups_color ON product_variant_groups (color);

-- 3) Add foreign keys to product_variants
ALTER TABLE product_variants
    ADD COLUMN size_id BIGINT NULL AFTER size,
    ADD COLUMN group_id BIGINT NULL AFTER product_id;

ALTER TABLE product_variants
    ADD CONSTRAINT fk_product_variants_size FOREIGN KEY (size_id) REFERENCES sizes (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_product_variants_group FOREIGN KEY (group_id) REFERENCES product_variant_groups (id) ON DELETE SET NULL;

CREATE INDEX idx_product_variants_size_id ON product_variants (size_id);
CREATE INDEX idx_product_variants_group_id ON product_variants (group_id);

-- 4) Backfill sizes from existing product_variants.size
INSERT INTO sizes (label)
SELECT DISTINCT pv.size
FROM product_variants pv
WHERE pv.size IS NOT NULL AND pv.size <> '';

-- 5) Backfill variant groups from existing product_variants (product_id + color)
INSERT INTO product_variant_groups (product_id, color, is_active)
SELECT pv.product_id, pv.color, TRUE
FROM product_variants pv
GROUP BY pv.product_id, pv.color;

-- 6) Link product_variants to size + group
UPDATE product_variants pv
JOIN sizes s ON s.label = pv.size
JOIN product_variant_groups g ON g.product_id = pv.product_id AND g.color = pv.color
SET pv.size_id = s.id,
    pv.group_id = g.id;

