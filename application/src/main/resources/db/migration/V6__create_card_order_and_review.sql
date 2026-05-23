CREATE TABLE card_order (
    id BIGSERIAL PRIMARY KEY,
    card_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    is_sample BOOLEAN NOT NULL DEFAULT FALSE,
    total_price INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    delivery_address VARCHAR(500) NOT NULL,
    selected_size VARCHAR(100) NOT NULL DEFAULT '',
    order_date TIMESTAMP NOT NULL DEFAULT NOW(),
    razorpay_order_id VARCHAR(255),
    razorpay_payment_id VARCHAR(255)
);

CREATE TABLE card_review (
    id BIGSERIAL PRIMARY KEY,
    card_id BIGINT NOT NULL REFERENCES invitation_card(id),
    customer_id BIGINT NOT NULL REFERENCES app_users(id),
    order_id BIGINT NOT NULL REFERENCES card_order(id),
    overall_rating INTEGER NOT NULL,
    print_quality_rating INTEGER NOT NULL,
    design_rating INTEGER NOT NULL,
    delivery_rating INTEGER NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
