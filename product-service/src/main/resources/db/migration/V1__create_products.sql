CREATE TABLE products (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_products_price_positive CHECK (price > 0)
);

CREATE INDEX idx_products_name ON products (name);
