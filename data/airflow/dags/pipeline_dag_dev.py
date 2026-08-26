"""VM-only dev integration DAG for the final project pipeline.

Manual trigger on the team Airflow VM: job-fp-ingest-dev, dev_airflow schema,
analytics_dev publish. Not loaded on Astro (laptops have DATABRICKS_TOKEN in
data/.env). For local DAG runs use final_project_pipeline in pipeline_dag.py.

HYF maintains this file. Edit pipeline_dag.py for your own DAG work.
"""

from __future__ import annotations

import os
import subprocess

import pendulum
from airflow.sdk import dag, task
from pipeline_dag import DEFAULT_ARGS, dbt_command, secret, setting, start_job


def databricks_environment_dev() -> dict[str, str]:
    catalog = setting("DATABRICKS_CATALOG")
    landing_default = f"/Volumes/{catalog}/landing/dev/aca-dev/postings"
    where = {
        "DATABRICKS_HOST": setting("DATABRICKS_HOST"),
        "DATABRICKS_HTTP_PATH": setting("DATABRICKS_HTTP_PATH"),
        "DATABRICKS_CATALOG": catalog,
        "DBT_SCHEMA": setting("DBT_SCHEMA_DEV", "dev_airflow"),
        "LANDING_PATH": setting("LANDING_PATH_DEV", landing_default),
    }
    team = setting("TEAM")
    return {
        **where,
        "AZURE_TENANT_ID": setting("AZURE_TENANT_ID"),
        "DATABRICKS_CLIENT_ID": secret("DATABRICKS_CLIENT_ID", f"fp-databricks-client-id-{team}"),
        "DATABRICKS_CLIENT_SECRET": secret(
            "DATABRICKS_CLIENT_SECRET", f"fp-databricks-client-secret-{team}"
        ),
    }


@dag(
    dag_id="final_project_pipeline_dev",
    description="Dev integration on the team VM: aca-dev landing, dev_airflow, analytics_dev publish",
    start_date=pendulum.datetime(2026, 1, 1, tz="Europe/Amsterdam"),
    schedule=None,
    catchup=False,
    max_active_runs=1,
    default_args=DEFAULT_ARGS,
    tags=["final-project", "dev"],
    is_paused_upon_creation=True,
)
def final_project_pipeline_dev():
    @task
    def ingest() -> str:
        job_name = setting("ACA_INGEST_JOB_DEV", "job-fp-ingest-dev")
        return start_job(job_name)

    @task
    def dbt_build() -> str:
        result = subprocess.run(
            dbt_command(),
            shell=True,
            check=False,
            env={**os.environ, **databricks_environment_dev()},
            text=True,
            capture_output=True,
            timeout=1800,
        )
        print(result.stdout[-8000:])
        if result.returncode != 0:
            print(result.stderr[-4000:])
            raise RuntimeError(f"dbt build exited {result.returncode}")

        summary = [line for line in result.stdout.splitlines() if "PASS=" in line]
        return summary[-1].strip() if summary else "dbt build finished"

    @task
    def publish_to_backend() -> int:
        from src.publishing import sync

        os.environ.update(databricks_environment_dev())

        for name, default in (
            ("BACKEND_PG_HOST", ""),
            ("BACKEND_PG_PORT", "5432"),
            ("BACKEND_PG_DB", ""),
            ("BACKEND_PG_SSLMODE", "require"),
        ):
            value = setting(name, default)
            if value:
                os.environ[name] = value

        os.environ["BACKEND_PG_USER"] = setting("BACKEND_PG_USER_DEV", "analytics_dev_user")
        os.environ["BACKEND_PG_PUBLISH_SCHEMA"] = setting(
            "BACKEND_PG_PUBLISH_SCHEMA_DEV", "analytics_dev"
        )

        if not os.environ.get("BACKEND_PG_PASSWORD"):
            team = setting("TEAM")
            secret_name = setting("BACKEND_PG_SECRET_DEV", "") or f"fp-pg-analytics-dev-team-{team}"
            os.environ["BACKEND_PG_PASSWORD"] = secret("BACKEND_PG_PASSWORD", secret_name)

        if hasattr(sync, "run_all"):
            counts = sync.run_all()
            return sum(counts.values())
        return sync.run()

    ingest() >> dbt_build() >> publish_to_backend()


if not os.environ.get("DATABRICKS_TOKEN"):
    final_project_pipeline_dev()
