CREATE SCHEMA IF NOT EXISTS order_service;

CREATE TABLE IF NOT EXISTS order_service.orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_price NUMERIC(19, 2) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'CART',
    delivery_address TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT orders_price_non_negative CHECK (total_price >= 0)
);