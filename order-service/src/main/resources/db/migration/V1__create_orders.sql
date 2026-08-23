CREATE TABLE IF NOT EXISTS t_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_number VARCHAR(36) NOT NULL,
    sku_code VARCHAR(100) NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    quantity INT NOT NULL,
    order_date DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_number (order_number),
    CONSTRAINT chk_orders_price_positive CHECK (price > 0),
    CONSTRAINT chk_orders_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_orders_sku_code ON t_orders (sku_code);
