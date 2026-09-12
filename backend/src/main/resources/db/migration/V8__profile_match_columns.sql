-- Adds the profile fields the form already collects and matching needs.
ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS discipline VARCHAR(255),
    ADD COLUMN IF NOT EXISTS work_mode VARCHAR(255),
    ADD COLUMN IF NOT EXISTS employment_type VARCHAR(255);
