CREATE TABLE otp_record (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(255) NOT NULL,
    otp VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE TABLE refresh_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id),
    token VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP NOT NULL
);
