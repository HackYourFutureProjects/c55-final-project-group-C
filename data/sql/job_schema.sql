CREATE TABLE sources (
    id INTEGER IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100),
    base_url TEXT
);

CREATE TABLE companies (
    id INTEGER IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(255) NOT null,
    type VARCHAR(100),
    base_url TEXT
);

CREATE TABLE jobs (
    id INTEGER IDENTITY(1,1) PRIMARY KEY,
    source_id INTEGER NOT NULL,
    source_job_id VARCHAR(255),
    company_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    experience_level VARCHAR(100),
    education_level VARCHAR(100),
    salary NUMERIC(12, 2),
    description TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(50),
    source_url TEXT
);

CREATE TABLE skills (
    id INTEGER IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

ALTER TABLE jobs
ADD CONSTRAINT fk_jobs_source
FOREIGN KEY (source_id)
REFERENCES sources(id);

ALTER TABLE jobs
ADD CONSTRAINT fk_jobs_company
FOREIGN KEY (company_id)
REFERENCES companies(id);

ALTER TABLE job_skills
ADD CONSTRAINT fk_job_skills_job
FOREIGN KEY (job_id)
REFERENCES jobs(id)
ON DELETE CASCADE;

ALTER TABLE job_skills
ADD CONSTRAINT fk_job_skills_skill
FOREIGN KEY (skill_id)
REFERENCES skills(id)
ON DELETE CASCADE;

SELECT
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
ORDER BY tc.table_name;