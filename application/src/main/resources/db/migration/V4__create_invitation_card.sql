CREATE TABLE invitation_card (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    image_url TEXT NOT NULL,
    price INTEGER NOT NULL,
    finish VARCHAR(50) NOT NULL,
    print_type VARCHAR(50) NOT NULL,
    size VARCHAR(255) NOT NULL,
    material VARCHAR(50) NOT NULL,
    paper_weight INTEGER NOT NULL,
    min_order_quantity INTEGER NOT NULL DEFAULT 1,
    tags VARCHAR(500) NOT NULL DEFAULT '',
    number_of_orders INTEGER NOT NULL DEFAULT 0,
    average_rating DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    review_count INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
