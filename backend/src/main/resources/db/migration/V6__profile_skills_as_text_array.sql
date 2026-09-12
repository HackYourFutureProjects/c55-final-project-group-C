-- Changes skills from jsonb to a real array, needed for array overlap against the mart.
-- No data to carry over - nothing wrote this column before /api/profile existed.
ALTER TABLE user_profiles
    ALTER COLUMN skills TYPE text[] USING '{}'::text[];
