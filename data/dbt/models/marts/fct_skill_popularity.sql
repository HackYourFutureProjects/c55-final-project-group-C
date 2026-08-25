-- One row per tag: how often it appears and when it was last seen.
--
-- The second reader of int_posting_tags, and the reason that model exists.
-- fct_postings asks "how many tags does this posting have?"; this asks "how
-- many postings does this tag have?". Same rows, counted along the other axis.
--
-- Not part of the backend contract. Airflow publishes fct_postings only, so
-- this mart is yours: query it in the warehouse, chart it, or add it to the
-- publish step if the product ends up needing it.
--
-- Change: delete it if your source has no tags, or point it at whatever your
-- fan-out model produces.
-- One row per skill:
-- number of jobs using the skill and its posting date range.

-- One row per skill.
-- Shows how many job postings contain each skill.

-- One row per skill.
-- Shows how many job postings contain each skill.

-- One row per skill.
-- Shows how many job postings contain each skill.

with postings as (

    select
        public_slug as posting_id,
        skills_raw as skills,
        posted_at

    from {{ ref("stg_postings") }}

),

exploded_skills as (

    select
        posting_id,
        posted_at,
        explode_outer(skills) as skill

    from postings

),

cleaned_skills as (

    select distinct
        posting_id,
        lower(trim(skill)) as skill,
        posted_at

    from exploded_skills

    where
        skill is not null
        and trim(skill) <> ''

)

select
    skill,
    count(distinct posting_id) as postings,
    min(posted_at) as first_seen_at,
    max(posted_at) as last_seen_at

from cleaned_skills

group by skill
