-- PRD 04/05/02: Partner profile fields, product lifecycle & sourcing, order item snapshots,
-- commission ledger stakeholder, stock audit log, referral_type HOTEL/GUIDE.

ALTER TABLE partners
    ADD COLUMN partner_type VARCHAR(20) NOT NULL DEFAULT 'HOTEL' AFTER display_name,
    ADD COLUMN email VARCHAR(255) NULL AFTER partner_type,
    ADD COLUMN commission_rate_percent DECIMAL(5, 2) NOT NULL DEFAULT 5.00 AFTER email;

UPDATE partners
SET partner_type = 'HOTEL',
    commission_rate_percent = 5.00
WHERE referral_code = 'HOTEL_ZHIWA_001';

ALTER TABLE products
    ADD COLUMN product_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER material,
    ADD COLUMN sourcing_type VARCHAR(20) NOT NULL DEFAULT 'OWNED' AFTER product_status,
    ADD COLUMN cost_price_btn DECIMAL(10, 2) NULL AFTER sourcing_type,
    ADD COLUMN consignment_commission_rate DECIMAL(7, 4) NULL AFTER cost_price_btn,
    ADD COLUMN weight_grams INT NULL AFTER consignment_commission_rate;

UPDATE products
SET product_status = IF(is_active, 'ACTIVE', 'ARCHIVED');

ALTER TABLE order_items
    ADD COLUMN sourcing_type VARCHAR(20) NOT NULL DEFAULT 'OWNED',
    ADD COLUMN consignment_commission_rate DECIMAL(7, 4) NULL;

UPDATE orders
SET referral_type = 'HOTEL'
WHERE referral_type = 'PARTNER';

ALTER TABLE commission_ledger_entries
    ADD COLUMN stakeholder VARCHAR(30) NULL AFTER order_id,
    ADD COLUMN order_item_id BIGINT NULL AFTER stakeholder,
    ADD COLUMN commission_rate_snapshot DECIMAL(10, 6) NULL AFTER amount_btn,
    ADD COLUMN base_amount_btn DECIMAL(12, 2) NULL AFTER commission_rate_snapshot;

UPDATE commission_ledger_entries
SET stakeholder = 'HOTEL_PARTNER'
WHERE partner_id IS NOT NULL;

ALTER TABLE commission_ledger_entries
    ADD CONSTRAINT fk_commission_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id);

CREATE INDEX idx_commission_order_item_id ON commission_ledger_entries (order_item_id);
CREATE INDEX idx_commission_stakeholder ON commission_ledger_entries (stakeholder);

CREATE TABLE stock_audit_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    variant_id      BIGINT          NOT NULL,
    change_type     VARCHAR(40)     NOT NULL,
    quantity_delta  INT             NOT NULL,
    available_after INT             NULL,
    reserved_after  INT             NULL,
    order_id        BIGINT          NULL,
    reason          VARCHAR(512)    NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_stock_audit_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_stock_audit_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_stock_audit_variant ON stock_audit_log (variant_id);
CREATE INDEX idx_stock_audit_created ON stock_audit_log (created_at);
