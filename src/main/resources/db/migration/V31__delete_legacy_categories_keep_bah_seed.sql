-- Remove legacy category rows and keep only the BAH taxonomy introduced in V30.
-- This intentionally hard-deletes old seed data instead of deactivating it.

DELETE FROM categories
WHERE slug NOT IN (
    'textiles',
    'buddhist-art',
    'home-decor',
    'jewelry-accessories',
    'wellness-edibles',
    'hand-woven-kira',
    'hand-woven-gho',
    'scarves-shawls',
    'yathra-textiles',
    'thangka-paintings',
    'ritual-items',
    'wood-crafts',
    'handmade-paper-crafts',
    'silver-jewelry',
    'artisan-bags-purses',
    'herbal-products',
    'artisanal-tea',
    'bumthang-yathra'
);
