-- One row per posting and city. Same pattern as int_posting_tags/skills:
-- the grain changes because cities_raw is an array, so it gets its own model
-- rather than a CTE inside int_postings.
--
-- Postings with an empty or null cities_raw simply don't appear here — that
-- is correct at this grain. A mart that needs to count postings regardless
-- of whether they have a parsed city should read int_postings.has_location_data
-- instead of expecting every posting_id to show up in this model.
--
-- No cleaning beyond trim/lowercase happens here. cities_raw is documented
-- in staging as not fully reliable, and deriving a single "final" city from
-- it is a separate, not-yet-built step — this model only explodes and
-- lightly normalizes what the source actually sent.
with
    postings as (select * from {{ ref("int_postings") }}),

    exploded as (
        select posting_id, posted_at, city
        from postings
        lateral view explode(cities_raw) as city
    ),

    cleaned as (
        select posting_id, posted_at, lower(trim(city)) as city
        from exploded
        where trim(city) <> ''
    ),

    deduplicated as (
        -- A source listing the same city twice on one posting shouldn't
        -- produce two rows for it.
        select *
        from cleaned
        qualify row_number() over (partition by posting_id, city order by posted_at) = 1
    )

select *
from deduplicated
