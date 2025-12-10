CREATE INDEX IF NOT EXISTS idx_product_service_products_shop_id ON product_service.products(shop_id);
CREATE INDEX IF NOT EXISTS idx_product_service_products_seller_id ON product_service.products(seller_id);
CREATE INDEX IF NOT EXISTS idx_product_service_products_status ON product_service.products(status);

CREATE INDEX IF NOT EXISTS idx_product_service_comments_product_id ON product_service.comments(product_id);
CREATE INDEX IF NOT EXISTS idx_product_service_comments_user_id ON product_service.comments(user_id);