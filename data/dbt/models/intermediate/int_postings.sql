-- int_postings.sql
-- One row per posting — same grain as stg_postings. Cleaning: HTML
-- stripped, work_mode normalized, and a flag for missing location data.
-- location_raw/countries_raw/regions_raw are carried through unchanged —
-- no intermediate model derives from them, but fct_postings does, so they
-- stay as plain pass-through columns. Everything else (city derivation,
-- salary, is_active, education, work arrangement detail, skills split,
-- matching score) is out of scope for this model — deliberately deferred,
-- not forgotten.
with
    postings as (select * from {{ ref("stg_postings") }}),

    keyed as (
        select
            -- Surrogate key so every downstream model (skills, cities,
            -- requirements) joins on one column instead of the natural
            -- (original_source, source_job_id) pair.
            md5(concat(original_source, '-', source_job_id)) as posting_id, *
        from postings
    ),

    description_cleaned as (
        select
            *,
            -- Strip tags, decode the handful of HTML entities FreeHire
            -- descriptions actually contain, then collapse whitespace.
            -- Stripping tags alone leaves entities like &#39; and &amp; as
            -- literal text in the output — this is not cosmetic, it's a
            -- correctness bug for anything downstream that reads the clean
            -- description as prose (search, display, matching).
            trim(
                regexp_replace(
                    replace(
                        replace(
                            replace(
                                replace(
                                    replace(
                                        replace(
                                            regexp_replace(
                                                description_raw, '<[^>]+>', ' '
                                            ),
                                            '&nbsp;',
                                            ' '
                                        ),
                                        '&amp;',
                                        '&'
                                    ),
                                    '&quot;',
                                    '"'
                                ),
                                '&#39;',
                                ''''
                            ),
                            '&lt;',
                            '<'
                        ),
                        '&gt;',
                        '>'
                    ),
                    '\\s+',
                    ' '
                )
            ) as description_clean
        from keyed
    ),

    work_mode_cleaned as (
        select
            *,
            -- Single source of truth for normalized work mode. Anything
            -- downstream that needs to group/filter on work mode should read
            -- this column, not re-derive lower(trim(source_work_mode))
            -- itself.
            nullif(lower(trim(source_work_mode)), '') as work_mode
        from description_cleaned
    ),

    flagged as (
        select
            *,
            -- All postings are 100% Netherlands, so country/region signal is
            -- irrelevant to whether a posting is usefully located. A posting
            -- has usable location signal if the source gave us a city, or if
            -- there's no city but the work is remote/hybrid (location doesn't
            -- matter for those). Onsite work with no city is genuinely
            -- unlocated — the mart decides what to do with it, not this
            -- model.
            (
                coalesce(size(cities_raw), 0) > 0
                or coalesce(work_mode in ('remote', 'hybrid'), false)
            ) as has_location_data
        from work_mode_cleaned
    )

select
    posting_id,
    source_job_id,
    original_source,
    public_slug,

    title,
    company_name,
    company_slug,
    source_url,

    description_raw,
    description_clean,

    cities_raw,
    location_raw,
    countries_raw,
    regions_raw,
    has_location_data,

    skills_raw,
    requirements_raw,

    work_mode,

    source_category,
    source_employment_type,
    source_experience_level,
    source_experience_years_min,
    source_education_level,
    source_posting_language,

    source_salary_min,
    source_salary_max,
    source_salary_currency,
    source_salary_period,

    source_company_type,
    source_company_size,
    source_is_tech,

    posted_at,
    source_created_at,
    updated_at,
    last_seen_at,
    closed_at,

    source_freshness_class,
    source_age_days,
    source_repost_count,
    source_mass_posting_count,
    source_fake_freshness,

    source_file,
    ingest_date,
    ingested_at

from flagged
