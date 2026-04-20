CREATE TABLE product_images (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id    BIGINT       NOT NULL,
    filename      VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_path     TEXT         NOT NULL,
    file_size     BIGINT,
    mime_type     VARCHAR(100),
    display_order INT          DEFAULT 0,
    uploaded_at   DATETIME(6)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,

    INDEX idx_product_images_product_id (product_id),

    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE CASCADE
);
