-- Migration to add KYC and verification columns to henna_artist
ALTER TABLE henna_artist ADD COLUMN IF NOT EXISTS id_front_url TEXT;
ALTER TABLE henna_artist ADD COLUMN IF NOT EXISTS id_back_url TEXT;
ALTER TABLE henna_artist ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE;
