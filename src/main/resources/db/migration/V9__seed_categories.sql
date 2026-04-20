-- Seed 10 categories for a modern clothing store (AttireHub-style)
-- Roots first (parent_id NULL), then children so parent IDs exist.

-- Root categories (id 1–4)
INSERT INTO categories (name, slug, description, parent_id, display_order, is_active) VALUES
('Women', 'women', 'Women''s clothing and accessories', NULL, 0, TRUE),
('Men', 'men', 'Men''s clothing and accessories', NULL, 1, TRUE),
('Kids', 'kids', 'Clothing for children', NULL, 2, TRUE),
('Accessories', 'accessories', 'Bags, shoes, and accessories', NULL, 3, TRUE);

-- Child categories (parent_id references roots above)
INSERT INTO categories (name, slug, description, parent_id, display_order, is_active) VALUES
('Dresses', 'womens-dresses', 'Dresses and gowns', 1, 0, TRUE),
('Tops & Shirts', 'womens-tops', 'Tops, blouses, and shirts', 1, 1, TRUE),
('Pants & Jeans', 'mens-pants', 'Trousers, jeans, and chinos', 2, 0, TRUE),
('Shirts & Polos', 'mens-shirts', 'Shirts and polos', 2, 1, TRUE),
('Shoes', 'shoes', 'Footwear for all', 4, 0, TRUE),
('Bags', 'bags', 'Handbags and backpacks', 4, 1, TRUE);
