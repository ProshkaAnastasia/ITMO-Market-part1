CREATE TABLE product_service.comments (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES product_service.products(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    text TEXT NOT NULL,
    rating INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT comments_rating_range CHECK (rating >= 1 AND rating <= 5)
);