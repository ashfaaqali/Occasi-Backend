-- Add created_at columns missing from original migrations
ALTER TABLE otp_record ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE refresh_token ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE artist_refresh_token ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
