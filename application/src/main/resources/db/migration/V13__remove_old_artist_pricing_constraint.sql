-- Migration to drop the obsolete Hibernate unique constraint on artist_pricing(artist_id, complexity_tier)
-- This allows having separate pricing rows for HAND and FEET designs under the same complexity tier.

ALTER TABLE artist_pricing DROP CONSTRAINT IF EXISTS ukcxa5wnjpcr49isu7q4a9t4g5h;
