-- One row per posting and requirement. requirements_raw is an array of
-- structs (priority, text) from FreeHire enrichment, so this changes grain
-- the same way skills and cities do, and gets its own model for the same
-- reason: fct_postings will want to count required vs preferred requirements
-- per posting, and any other future reader needs the same split rather than
-- re-deriving it.
--
-- Postings with an empty or null requirements_raw don't appear here. That's
-- correct at this grain — a mart counting requirements per posting has to
-- coalesce the count back to 0 for those postings, same as skills.
with
    postings as (select * from {{ ref("int_postings") }}),

    exploded as (
        select
            posting_id,
            posted_at,
            req.priority as priority_raw,
            req.text as requirement_text_raw
        from postings
        lateral view explode(requirements_raw) as req
    ),

    cleaned as (
        select
            posting_id,
            posted_at,
            -- Not assuming the source's casing is consistent — normalize to
            -- lowercase so 'Required' and 'required' don't become two values
            -- a mart has to handle separately.
            lower(trim(priority_raw)) as priority,
            trim(requirement_text_raw) as requirement_text
        from exploded
        where trim(requirement_text_raw) <> ''
    ),

    deduplicated as (
        -- Same requirement text listed twice under the same priority on one
        -- posting shouldn't produce two rows.
        select *
        from cleaned
        qualify
            row_number() over (
                partition by posting_id, priority, requirement_text order by posted_at
            )
            = 1
    )

select *
from deduplicated
