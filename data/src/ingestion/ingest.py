"""Fetch records from the source API and validate them.

The default source is the Arbeitnow job board, which needs no API key. Point
SOURCE_API_URL at your team's source and rewrite `parse_records` to match it.
"""

import time
import logging
from typing import Any

import requests
from pydantic import ValidationError

from .models import Posting

logger = logging.getLogger(__name__)

REQUEST_TIMEOUT_SECONDS = 30
BASE_URL = "https://freehire.me/api/v1/jobs/search"
MAX_RETRIES = 3
DEFAULT_LIMIT = 100
MAX_OFFSET_LIMIT = 10000
RETRYABLE_STATUS_CODES = {429, 500, 502, 503, 504}


def fetch_with_retry(url: str, params: dict, max_retries: int = MAX_RETRIES) -> dict:
    """Fetch a URL with exponential backoff on failure and return JSON payload."""
    for attempt in range(max_retries):
        try:
            response = requests.get(url, params=params, timeout=REQUEST_TIMEOUT_SECONDS)
            response.raise_for_status()
            return response.json()
        except (
            requests.exceptions.ConnectionError,
            requests.exceptions.Timeout,
            requests.exceptions.HTTPError,
        ) as e:
            # Check if it's a 5xx server error or a rate limit (429), which are worth retrying
            if (
                isinstance(e, requests.exceptions.HTTPError)
                and e.response.status_code not in RETRYABLE_STATUS_CODES
            ):
                logger.error("Client error encountered for %s: %s. Not retrying.", url, e)
                raise  # Do not retry client errors like 400 or 404

            if attempt == max_retries - 1:
                logger.error("Failed to fetch %s after %d attempts. Error: %s", url, max_retries, e)
                raise

            wait_time = 2**attempt  # exponential backoff
            logger.warning(
                "Attempt %d failed for %s, retrying in %s seconds...", attempt + 1, url, wait_time
            )
            time.sleep(wait_time)


def fetch_raw(url: str = BASE_URL) -> list[Any]:
    """Call the freehire.me API using limit/offset pagination linked to retry logic."""
    all_records = []
    offset = 0
    limit = DEFAULT_LIMIT
    max_offset_limit = MAX_OFFSET_LIMIT

    while True:
        if offset + limit > max_offset_limit:
            logger.info(
                "Reached maximum allowed offset boundary (%d). Stopping pagination.",
                max_offset_limit,
            )
            break

        logger.info("Fetching %s (offset=%d, limit=%d)", url, offset, limit)
        # added parameter to filter for Netherlands only, offset and limits to match api pagination and limits
        params = {"offset": offset, "limit": limit, "countries": "NL"}
        payload = fetch_with_retry(url, params)

        if isinstance(payload, dict):
            records = payload.get("data", payload.get("results", payload))
            meta = payload.get("meta", {})
            total_matching = meta.get("total")
            if total_matching is not None and offset >= total_matching:
                break
        else:
            records = payload

        if not isinstance(records, list):
            raise TypeError(f"Expected a list of records, got {type(records).__name__}")

        if not records:
            break

        all_records.extend(records)
        logger.info("Received %d record(s) (Total collected: %d)", len(records), len(all_records))
        if len(records) < limit:
            break

        offset += limit

    logger.info("Total received: %d record(s)", len(all_records))
    return all_records


def parse_records(records: list[Any]) -> tuple[list[Posting], int]:
    """Validate raw records, returning the good ones and a rejected count.

    One malformed record must not lose the whole batch, so invalid rows are
    counted and skipped. `Any` is deliberate: this is the boundary, and the
    source can send anything.
    """
    parsed: list[Posting] = []
    rejected = 0
    for record in records:
        try:
            parsed.append(Posting.model_validate(record))
        except ValidationError as exc:
            rejected += 1
            # A JSON list can hold a scalar, and .get on one would raise here
            # and lose the batch this loop exists to save.
            identifier = (
                record.get("slug", "<no slug>") if isinstance(record, dict) else repr(record)[:40]
            )
            logger.warning("Rejected record %s: %s", identifier, exc.error_count())
    logger.info("Parsed %d record(s), rejected %d", len(parsed), rejected)
    return parsed, rejected
