CREATE TABLE IF NOT EXISTS t_inventory (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku_code VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_sku_code (sku_code),
    CONSTRAINT chk_inventory_quantity_non_negative CHECK (quantity >= 0)
);

CREATE INDEX idx_inventory_sku_quantity ON t_inventory (sku_code, quantity);
