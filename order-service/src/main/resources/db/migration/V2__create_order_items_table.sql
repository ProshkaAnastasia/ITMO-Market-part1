CREATE TABLE IF NOT EXISTS order_service.order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES order_service.orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    price NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(order_id, product_id),
    CONSTRAINT order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT order_items_price_positive CHECK (price > 0)
);