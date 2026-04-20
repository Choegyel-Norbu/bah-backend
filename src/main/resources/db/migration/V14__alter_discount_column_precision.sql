-- Increase discount column precision to allow larger values (was DECIMAL(5,2), max 999.99)
ALTER TABLE product_variants MODIFY COLUMN discount DECIMAL(10, 2) NOT NULL DEFAULT 0;
