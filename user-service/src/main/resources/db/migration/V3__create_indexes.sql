CREATE INDEX IF NOT EXISTS idx_user_service_users_username ON user_service.users(username);
CREATE INDEX IF NOT EXISTS idx_user_service_users_email ON user_service.users(email);

CREATE INDEX IF NOT EXISTS idx_user_service_shops_seller_id ON user_service.shops(seller_id);