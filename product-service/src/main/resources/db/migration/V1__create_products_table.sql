CREATE SCHEMA IF NOT EXISTS product_service;

CREATE TABLE IF NOT EXISTS product_service.products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(19, 2) NOT NULL,
    image_url VARCHAR(500),
    shop_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT products_price_positive CHECK (price > 0)
);