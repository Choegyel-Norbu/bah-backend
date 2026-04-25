-- Partners (hotels, guides) for referral attribution (PRD Flow 2)
CREATE TABLE partners (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    referral_code   VARCHAR(100)    NOT NULL,
    display_name    VARCHAR(255)    NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    total_clicks    INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_partners_referral_code UNIQUE (referral_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_partners_status ON partners (status);

-- Referral click audit log
CREATE TABLE referral_clicks (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    partner_id       BIGINT          NOT NULL,
    referral_code    VARCHAR(100)    NOT NULL,
    user_agent_hash  VARCHAR(64),
    ip_hash          VARCHAR(64),
    session_id       VARCHAR(128),
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_referral_clicks_partner FOREIGN KEY (partner_id) REFERENCES partners (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_referral_clicks_partner_id ON referral_clicks (partner_id);
CREATE INDEX idx_referral_clicks_created_at ON referral_clicks (created_at);

-- Stock reserved for unpaid Stripe checkouts (PRD inventory)
ALTER TABLE product_variants
    ADD COLUMN reserved_quantity INT NOT NULL DEFAULT 0 AFTER stock_quantity;

-- Orders: longer status values, referral lock, Stripe, fulfillment (PRD Flows 1, 4)
ALTER TABLE orders
    MODIFY COLUMN status VARCHAR(40) NOT NULL DEFAULT 'PENDING';

ALTER TABLE orders
    ADD COLUMN referral_type VARCHAR(20) NOT NULL DEFAULT 'NONE' AFTER coupon_code,
    ADD COLUMN referral_code VARCHAR(100) NULL AFTER referral_type,
    ADD COLUMN referral_partner_id BIGINT NULL AFTER referral_code,
    ADD COLUMN referral_partner_display_name VARCHAR(255) NULL AFTER referral_partner_id,
    ADD COLUMN exchange_rate_used DECIMAL(18, 6) NULL AFTER referral_partner_display_name,
    ADD COLUMN stripe_payment_intent_id VARCHAR(255) NULL AFTER exchange_rate_used,
    ADD COLUMN charged_currency VARCHAR(3) NULL AFTER stripe_payment_intent_id,
    ADD COLUMN tracking_number VARCHAR(128) NULL AFTER charged_currency,
    ADD COLUMN delivered_at TIMESTAMP NULL AFTER tracking_number;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_referral_partner FOREIGN KEY (referral_partner_id) REFERENCES partners (id);

CREATE UNIQUE INDEX uk_orders_stripe_payment_intent ON orders (stripe_payment_intent_id);

-- Payment records (PRD: Payment status CREATED → SUCCEEDED)
CREATE TABLE payments (
    id                        BIGINT          NOT NULL AUTO_INCREMENT,
    order_id                  BIGINT          NOT NULL,
    stripe_payment_intent_id  VARCHAR(255)    NOT NULL,
    status                    VARCHAR(20)     NOT NULL,
    amount_minor              BIGINT          NOT NULL,
    currency                  VARCHAR(3)      NOT NULL,
    created_at                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_payments_stripe_pi UNIQUE (stripe_payment_intent_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payments_order_id ON payments (order_id);

-- Commission ledger (PRD: entries on successful payment; reversals later)
CREATE TABLE commission_ledger_entries (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    order_id     BIGINT          NOT NULL,
    partner_id   BIGINT          NULL,
    entry_type   VARCHAR(20)     NOT NULL,
    amount_btn   DECIMAL(12, 2)  NOT NULL,
    description  VARCHAR(512)    NULL,
    created_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_commission_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_commission_partner FOREIGN KEY (partner_id) REFERENCES partners (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_commission_order_id ON commission_ledger_entries (order_id);

-- Return / refund requests (PRD Flow 5 — intake)
CREATE TABLE order_return_requests (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    order_id        BIGINT          NOT NULL,
    customer_email  VARCHAR(255)    NOT NULL,
    reason          VARCHAR(40)     NOT NULL,
    item_variant_ids VARCHAR(512)   NOT NULL,
    photo_urls      TEXT            NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    admin_notes     TEXT            NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_return_requests_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_return_requests_order_id ON order_return_requests (order_id);
CREATE INDEX idx_return_requests_status ON order_return_requests (status);

-- Dev seed partner (safe for empty DB)
INSERT INTO partners (referral_code, display_name, status, total_clicks)
VALUES ('HOTEL_ZHIWA_001', 'Zhiwa Ling Heritage Hotel', 'ACTIVE', 0);
