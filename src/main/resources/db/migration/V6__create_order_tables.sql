-- Orders table
CREATE TABLE orders (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    order_number        VARCHAR(50)     NOT NULL,
    user_id             BIGINT          NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    subtotal            DECIMAL(10, 2)  NOT NULL,
    discount            DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    tax                 DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    shipping_cost       DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    total               DECIMAL(10, 2)  NOT NULL,
    payment_method      VARCHAR(30)     NOT NULL DEFAULT 'CASH_ON_DELIVERY',
    payment_status      VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    shipping_address_id BIGINT,
    coupon_code         VARCHAR(50),
    notes               TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT uk_orders_order_number UNIQUE (order_number),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_shipping_address FOREIGN KEY (shipping_address_id) REFERENCES addresses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes
CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at);

-- Order items table
CREATE TABLE order_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    order_id        BIGINT          NOT NULL,
    variant_id      BIGINT          NOT NULL,
    product_name    VARCHAR(255)    NOT NULL,
    sku             VARCHAR(100)    NOT NULL,
    size            VARCHAR(10)     NOT NULL,
    color           VARCHAR(50)     NOT NULL,
    quantity        INT             NOT NULL,
    unit_price      DECIMAL(10, 2)  NOT NULL,
    total_price     DECIMAL(10, 2)  NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes
CREATE INDEX idx_order_items_order_id ON order_items (order_id);

-- Order status history (audit trail)
CREATE TABLE order_status_history (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    order_id        BIGINT          NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    notes           TEXT,
    changed_by      BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes
CREATE INDEX idx_order_status_history_order_id ON order_status_history (order_id);
