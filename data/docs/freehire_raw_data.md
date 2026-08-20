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

Incoming records are validated before being written to the raw data layer.

Required fields are:

- public_slug
- title
- company
- url
- created_at

The following fields are preserved and used when available:

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

Some FreeHire records can contain missing or empty values for these fields.
A missing optional field does not make the complete job record invalid.

Invalid records are rejected and counted without stopping the complete batch.

## Validation result

The FreeHire validation is covered by automated ingestion tests.

Current test result:

- ingestion tests: 7 passed

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