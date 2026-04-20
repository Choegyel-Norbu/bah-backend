-- Remove product-level image; images are stored only on variants (product_variants.image_url).
ALTER TABLE products DROP COLUMN image_url;

-- Drop product_images table (images are variant-level only).
DROP TABLE IF EXISTS product_images;
