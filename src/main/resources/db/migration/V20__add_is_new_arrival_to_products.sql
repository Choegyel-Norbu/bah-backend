-- Add is_new_arrival flag to products (admin-set; replaces computed "last 7 days" logic)
ALTER TABLE products
    ADD COLUMN is_new_arrival BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_products_is_new_arrival ON products (is_new_arrival);
