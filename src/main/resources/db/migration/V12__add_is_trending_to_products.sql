-- Add is_trending column so shop owners can manually mark products as trending
ALTER TABLE products ADD COLUMN is_trending BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_products_is_trending ON products (is_trending);
