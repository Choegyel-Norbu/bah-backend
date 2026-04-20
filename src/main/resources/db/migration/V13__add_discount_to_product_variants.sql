-- Add discount percentage (0-100) to product variants
ALTER TABLE product_variants ADD COLUMN discount DECIMAL(5, 2) NOT NULL DEFAULT 0;
