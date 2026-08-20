# FreeHire Raw Data

## Source

The job data is collected from the FreeHire API.

The ingestion pipeline retrieves job records for the Netherlands.

## Raw data format

The raw data is stored as NDJSON (newline-delimited JSON).

Each line contains one original job record from the FreeHire API.

The raw data is stored without transformations or cleaning.

Example raw fields include:

- public_slug
- source
- external_id
- url
- title
- company
- location
- countries
- regions
- cities
- work_mode
- skills
- posted_at
- created_at
- updated_at
- last_seen_at
- closed_at
- enrichment
- reality

## Validation

Incoming FreeHire records are validated by `parse_records()` using the
`Posting` Pydantic model.

For each record, the validation checks that:

- the record is a JSON object/dictionary;
- the record matches the expected FreeHire job structure;
- required fields are present;
- `public_slug` is unique within the batch.

Required fields are:

- public_slug
- title
- company
- url
- created_at

The following fields are preserved when available:

- location
- countries
- regions
- cities
- work_mode
- skills
- posted_at
- updated_at
- last_seen_at
- closed_at

Some FreeHire records can contain missing or empty values for these optional
fields. A missing optional field does not make the complete job record invalid.

Invalid or duplicate records are counted as rejected without stopping the
validation of the remaining records.

## Validation result

The FreeHire validation is covered by automated ingestion tests.

Current test result:

- ingestion tests: 8 passed

The tests cover:

- valid FreeHire records;
- missing required fields;
- invalid non-object records;
- optional missing or empty fields;
- datetime parsing;
- `closed_at = null`;
- batch processing with invalid records;
- duplicate `public_slug` values.

The validation has also been tested with real FreeHire job records through the
integrated ingestion pipeline.

## Ingestion metadata

The raw data path contains information about the source and ingestion date.

Example:

local-landing/<user>/freehire/ingest_date=YYYY-MM-DD/data.json

The path identifies:

- source: freehire
- ingestion date

The pipeline logs additional ingestion information, including:

- number of received records
- number of parsed records
- number of rejected records
- number of written records

The original FreeHire job records are not modified to add ingestion metadata.

## Data quality observations

During validation of the FreeHire data, several differences in data completeness
and representation were observed:

- `cities` can be empty.
- `skills` can be empty.
- `work_mode` can be missing.
- `location` is not always represented in the same format.
- `cities` may sometimes contain values that are not strictly city names.
- `closed_at` can be null.
- A job can have `closed_at = null` while its `reality.class` is `stale`.

All available values are preserved in the raw data layer.

Cleaning, standardisation, and fallback logic should be handled later in the
transformation or staging layer.