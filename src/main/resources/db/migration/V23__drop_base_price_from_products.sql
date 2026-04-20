-- Remove base_price from products; price is stored per variant only.
DROP INDEX idx_products_base_price ON products;
ALTER TABLE products DROP COLUMN base_price;
