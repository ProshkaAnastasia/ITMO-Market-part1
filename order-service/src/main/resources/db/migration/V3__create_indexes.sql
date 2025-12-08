CREATE INDEX idx_order_service_orders_user_id ON order_service.orders(user_id);
CREATE INDEX idx_order_service_orders_status ON order_service.orders(status);

CREATE INDEX idx_order_service_order_items_order_id ON order_service.order_items(order_id);
CREATE INDEX idx_order_service_order_items_product_id ON order_service.order_items(product_id);