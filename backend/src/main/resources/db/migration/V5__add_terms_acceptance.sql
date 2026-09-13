-- GDPR: don't store personal data before the user agrees to terms.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS terms_accepted_at TIMESTAMPTZ;
