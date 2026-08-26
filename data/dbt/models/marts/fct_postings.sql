-- This mart is the contract with the backend team.
--
-- Its columns are what backend/ reads to build API endpoints, so treat a change
-- here the way you would treat changing a public API: agree it with the backend
-- trainees first, then change it in both places.
--
-- Airflow copies this table into the backend's database after dbt succeeds, so
-- whatever you select here is what they get.
--
-- Change: rename to your domain and decide the grain. Write one sentence in
-- _fct_postings.yml saying what one row means. If you cannot write that
-- sentence, the mart is not ready.
with postings as (select * from {{ ref("stg_postings") }})

select
    public_slug as posting_id,

    original_source as source,
    source_job_id,

    title,
    company_name,

    location_raw as location,
    countries_raw as countries,
    regions_raw as regions,
    cities_raw as cities,

    source_work_mode as work_mode,

    case
        when lower(coalesce(source_work_mode, '')) = 'remote' then true else false
    end as is_remote,

    skills_raw as skills,

    coalesce(size(array_distinct(skills_raw)), 0) as skill_count,

    source_experience_level as experience_level,
    source_education_level as education_level,
    source_employment_type as employment_type,

    source_salary_min as salary_min,
    source_salary_max as salary_max,
    source_salary_currency as salary_currency,
    source_salary_period as salary_period,

    source_category as category,

    description_raw as description,

    posted_at,
    date(posted_at) as posted_date,

    updated_at,
    last_seen_at,
    closed_at,

    case when closed_at is null then 'open' else 'closed' end as status,

    source_freshness_class as freshness_class,
    source_age_days as age_days,
    source_repost_count as repost_count,
    source_fake_freshness as fake_freshness,

    source_url,

    ingest_date,
    ingested_at

from postings
