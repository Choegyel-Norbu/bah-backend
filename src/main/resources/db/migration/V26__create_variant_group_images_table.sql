-- Create table for variant group images (1:N from product_variant_groups to variant_group_images)
CREATE TABLE variant_group_images (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    group_id    BIGINT          NOT NULL,
    image_url   VARCHAR(500)    NOT NULL,
    is_primary  BOOLEAN         NOT NULL DEFAULT FALSE,
    sort_order  INT             NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_variant_group_images_group FOREIGN KEY (group_id)
        REFERENCES product_variant_groups (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_variant_group_images_group_id ON variant_group_images (group_id);
CREATE INDEX idx_variant_group_images_sort_order ON variant_group_images (group_id, sort_order);

