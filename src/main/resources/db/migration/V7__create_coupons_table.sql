-- Coupons table
CREATE TABLE coupons (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    code                    VARCHAR(50)     NOT NULL,
    description             TEXT,
    discount_type           VARCHAR(20)     NOT NULL,
    discount_value          DECIMAL(10, 2)  NOT NULL,
    minimum_order_amount    DECIMAL(10, 2)  DEFAULT NULL,
    max_discount_amount     DECIMAL(10, 2)  DEFAULT NULL,
    usage_limit             INT             DEFAULT NULL,
    times_used              INT             NOT NULL DEFAULT 0,
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    valid_from              TIMESTAMP       NOT NULL,
    valid_until             TIMESTAMP       NOT NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT uk_coupons_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes
CREATE INDEX idx_coupons_code ON coupons (code);
CREATE INDEX idx_coupons_is_active ON coupons (is_active);
