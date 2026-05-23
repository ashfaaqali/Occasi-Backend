CREATE TABLE henna_artist (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    mobile_number VARCHAR(255) NOT NULL,
    city_name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    rating SMALLINT NOT NULL DEFAULT 0,
    reviews INTEGER NOT NULL DEFAULT 0,
    cover_image TEXT,
    starting_price INTEGER NOT NULL DEFAULT 0,
    password_hash VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE artist_portfolio_image (
    id BIGSERIAL PRIMARY KEY,
    artist_id BIGINT NOT NULL REFERENCES henna_artist(id),
    image_url TEXT NOT NULL
);

CREATE TABLE artist_pricing (
    id BIGSERIAL PRIMARY KEY,
    artist_id BIGINT NOT NULL REFERENCES henna_artist(id),
    complexity_tier VARCHAR(50) NOT NULL,
    price INTEGER NOT NULL
);

CREATE TABLE artist_refresh_token (
    id BIGSERIAL PRIMARY KEY,
    artist_id BIGINT NOT NULL REFERENCES henna_artist(id),
    token VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP NOT NULL
);
