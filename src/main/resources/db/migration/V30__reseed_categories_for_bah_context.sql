-- Re-seed categories for Bhutan Artisan Hub context.
-- Keep this idempotent so it is safe across environments.

-- 1) Deactivate legacy clothing-store seed categories (V9/V10).
UPDATE categories
SET is_active = FALSE
WHERE slug IN (
    'women', 'men', 'kids', 'accessories',
    'womens-dresses', 'womens-tops', 'mens-pants', 'mens-shirts', 'shoes', 'bags',
    'womens-skirts', 'womens-sweaters', 'womens-activewear', 'womens-swimwear',
    'mens-jackets', 'mens-shorts', 'mens-activewear',
    'kids-boys', 'kids-girls', 'kids-baby',
    'jewelry', 'watches', 'hats-caps', 'belts', 'sunglasses',
    'casual-dresses', 'evening-dresses', 'formal-shirts', 'sneakers', 'boots'
);

-- 2) Seed new root categories.
INSERT INTO categories (name, slug, description, parent_id, display_order, is_active)
VALUES
    ('Textiles', 'textiles', 'Traditional Bhutanese woven textiles and garments', NULL, 0, TRUE),
    ('Buddhist Art', 'buddhist-art', 'Spiritual and ritual art pieces from Bhutan', NULL, 1, TRUE),
    ('Home Decor', 'home-decor', 'Handcrafted decor and household artisan products', NULL, 2, TRUE),
    ('Jewelry & Accessories', 'jewelry-accessories', 'Artisan jewelry and personal accessories', NULL, 3, TRUE),
    ('Wellness & Edibles', 'wellness-edibles', 'Bhutanese herbal, tea, and edible artisan products', NULL, 4, TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    parent_id = VALUES(parent_id),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);

-- 3) Seed level-2 categories.
INSERT INTO categories (name, slug, description, parent_id, display_order, is_active)
SELECT
    'Hand-woven Kira', 'hand-woven-kira',
    'Traditional hand-woven Kira for women',
    p.id, 0, TRUE
FROM categories p
WHERE p.slug = 'textiles'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    parent_id = VALUES(parent_id),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);

INSERT INTO categories (name, slug, description, parent_id, display_order, is_active)
SELECT
    'Hand-woven Gho', 'hand-woven-gho',
    'Traditional hand-woven Gho for men',
    p.id, 1, TRUE
FROM categories p
WHERE p.slug = 'textiles'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    parent_id = VALUES(parent_id),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);

INSERT INTO categories (name, slug, description, parent_id, display_order, is_active)
SELECT
    'Scarves & Shawls', 'scarves-shawls',
    'Handwoven scarves, stoles, and shawls',
    p.id, 2, TRUE
FROM categories p
WHERE p.slug = 'textiles'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    parent_id = VALUES(parent_id),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);

INSERT INTO categories (name, slug, description, parent_id, display_order, is_active)
SELECT
    'Yathra Textiles', 'yathra-textiles',
    'Woolen Yathra weaving and textile products',
    p.id, 3, TRUE
FROM categories p
WHERE p.slug = 'textiles'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    parent_id = VALUES(parent_id),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);

INSERT INTO categories (name, slug, description, parent_id, display_order, is_active)
SELECT
    'Thangka Paintings', 'thangka-paintings',
    'Traditional Bhutanese and Himalayan thangka art',
    p.id, 0, TRUE
FROM categories p
WHERE p.slug = 'buddhist-art'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    parent_id = VALUES(parent_id),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);

INSERT INTO categories (name, slug, description, parent_id, display_order, is_active)
SELECT
    'Ritual Items', 'ritual-items',
    'Prayer wheels, offering bowls, and ceremonial items',
    p.id, 1, TRUE
FROM categories p
WHERE p.slug = 'buddhist-art'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    parent_id = VALUES(parent_id),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);

INSERT INTO categories (name, slug, description, parent_id, display_order, is_active)
SELECT
    'Wood Crafts', 'wood-crafts',
    'Hand-carved wooden artisan products',
    p.id, 0, TRUE
FROM categories p
WHERE p.slug = 'home-decor'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    parent_id = VALUES(parent_id),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);

INSERT INTO categories (name, slug, description, parent_id, display_order, is_active)
SELECT
    'Handmade Paper Crafts', 'handmade-paper-crafts',
    'Handmade paper journals, cards, and decor',
    p.id, 1, TRUE
FROM categories p
WHERE p.slug = 'home-decor'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    parent_id = VALUES(parent_id),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);

INSERT INTO categories (name, slug, description, parent_id, display_order, is_active)
SELECT
    'Silver Jewelry', 'silver-jewelry',
    'Traditional and contemporary artisan silver jewelry',
    p.id, 0, TRUE
FROM categories p
WHERE p.slug = 'jewelry-accessories'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    parent_id = VALUES(parent_id),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);

INSERT INTO categories (name, slug, description, parent_id, display_order, is_active)
SELECT
    'Bags & Purses', 'artisan-bags-purses',
    'Textile and leather artisan bags and purses',
    p.id, 1, TRUE
FROM categories p
WHERE p.slug = 'jewelry-accessories'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    parent_id = VALUES(parent_id),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);

INSERT INTO categories (name, slug, description, parent_id, display_order, is_active)
SELECT
    'Herbal Products', 'herbal-products',
    'Natural wellness products made in Bhutan',
    p.id, 0, TRUE
FROM categories p
WHERE p.slug = 'wellness-edibles'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    parent_id = VALUES(parent_id),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);

INSERT INTO categories (name, slug, description, parent_id, display_order, is_active)
SELECT
    'Artisanal Tea', 'artisanal-tea',
    'Local Bhutanese artisan teas and infusions',
    p.id, 1, TRUE
FROM categories p
WHERE p.slug = 'wellness-edibles'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    parent_id = VALUES(parent_id),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);

-- 4) Seed level-3 category from PRD example:
-- Textiles > Hand-woven Kira > Bumthang Yathra
INSERT INTO categories (name, slug, description, parent_id, display_order, is_active)
SELECT
    'Bumthang Yathra', 'bumthang-yathra',
    'Bumthang-origin Yathra weave products and garments',
    p.id, 0, TRUE
FROM categories p
WHERE p.slug = 'hand-woven-kira'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    parent_id = VALUES(parent_id),
    display_order = VALUES(display_order),
    is_active = VALUES(is_active);
