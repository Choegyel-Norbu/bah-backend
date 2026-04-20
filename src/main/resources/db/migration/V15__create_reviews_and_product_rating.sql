-- Reviews: only users who purchased the product can leave a review (verified purchase).
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating TINYINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment VARCHAR(2000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_review_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_review_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_reviews_product_id ON reviews (product_id);
CREATE INDEX idx_reviews_user_id ON reviews (user_id);

-- Product aggregate fields for rating (updated when reviews change).
ALTER TABLE products
    ADD COLUMN average_rating DECIMAL(3,2) NULL,
    ADD COLUMN review_count INT NOT NULL DEFAULT 0;

CREATE INDEX idx_products_average_rating ON products (average_rating);
