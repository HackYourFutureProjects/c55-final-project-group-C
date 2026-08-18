ALTER TABLE users
    ALTER COLUMN id SET DEFAULT gen_random_uuid(),
    ALTER COLUMN email TYPE VARCHAR(255),
    ALTER COLUMN email SET NOT NULL,
    ADD CONSTRAINT users_email_unique UNIQUE (email),
    ADD COLUMN name VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE TABLE user_credentials (
    user_id UUID PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    refresh_token TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_credentials FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY,
    prefered_role VARCHAR(255),
    prefered_city VARCHAR(255),
    prefered_country VARCHAR(255),
    education_level VARCHAR(255),
    experience_level VARCHAR(255),
    salary NUMERIC(10, 2),
    skills JSONB,
    CONSTRAINT fk_user_profile FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TYPE job_state AS ENUM (
    'APPLIED',
    'REJECTED',
    'ACCEPTED',
    'DECLINED'
);

CREATE TABLE saved_jobs (
    user_id UUID NOT NULL,
    job_id INT NOT NULL,
    saved_status VARCHAR(50),
    job_state job_state,
    PRIMARY KEY (user_id, job_id),
    CONSTRAINT fk_saved_jobs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
