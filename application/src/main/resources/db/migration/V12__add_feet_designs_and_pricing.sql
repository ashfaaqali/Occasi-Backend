-- Migration to support feet designs, coverage multipliers, and artist pricing separation

-- 1. Modify henna_design table
ALTER TABLE henna_design ADD COLUMN IF NOT EXISTS design_type VARCHAR(50) NOT NULL DEFAULT 'HAND';
ALTER TABLE henna_design DROP COLUMN IF EXISTS price;

-- 2. Modify artist_pricing table
ALTER TABLE artist_pricing ADD COLUMN IF NOT EXISTS design_type VARCHAR(50) NOT NULL DEFAULT 'HAND';

-- Add composite unique constraint
ALTER TABLE artist_pricing ADD CONSTRAINT unique_artist_complexity_type UNIQUE (artist_id, complexity_tier, design_type);

-- 3. Modify booking table
ALTER TABLE booking ADD COLUMN IF NOT EXISTS hand_design_id BIGINT REFERENCES henna_design(id);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS feet_design_id BIGINT REFERENCES henna_design(id);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS hand_coverage VARCHAR(50);

-- Migrate existing booking.design_id records to hand_design_id
UPDATE booking SET hand_design_id = design_id WHERE design_id IS NOT NULL;

-- Drop old design_id column
ALTER TABLE booking DROP COLUMN IF EXISTS design_id;
