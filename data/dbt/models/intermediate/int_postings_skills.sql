-- One row per posting and skill. Replaces int_posting_tags — staging has no
-- separate tags field, skills_raw was the only source array being exploded,
-- so this model absorbs that role under its correct name.
--
-- Same reasoning as int_posting_cities/requirements: skills_raw is an array
-- on a posting-grain row, so exploding it changes the grain, which is why
-- this is its own model rather than a CTE inside int_postings.
with
    postings as (select * from {{ ref("int_postings") }}),

    exploded as (

        -- `explode` drops postings whose skill array is empty or null, which is
        -- correct: a posting with no skills has no rows at this grain. It also
        -- means fct_postings has to put the zero back with a coalesce, rather
        -- than losing the posting from the mart entirely.
        select posting_id, posted_at, skill
        from postings
        lateral view explode(skills_raw) as skill

    ),

    cleaned as (

        select
            posting_id,
            -- Skills arrive as the source typed them, so "Python", "python" and
            -- " python" are three skills until you say otherwise. Normalising
            -- here means every consumer gets the same answer.
            lower(trim(skill)) as skill,
            posted_at
        from exploded
        where trim(skill) <> ''

    ),

    deduplicated as (

        -- A source that lists the same skill twice on one posting would otherwise
        -- double that posting's skill_count and its weight in the popularity
        -- mart.
        select *
        from cleaned
        qualify row_number() over (partition by posting_id, skill order by posted_at) = 1

    )

select *
from deduplicated