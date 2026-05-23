CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    mobile_number VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    google_id VARCHAR(255)
);
