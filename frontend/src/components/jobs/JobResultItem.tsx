import Link from "next/link";
import type { JobSearchResult } from "@/lib/jobs";

type JobResultItemProps = {
  job: JobSearchResult;
};

export default function JobResultItem({ job }: JobResultItemProps) {
  return (
    <article className="job-result-item">
      <div className="job-result-main">
        <div className="job-result-heading">
          <div>
            <p className="job-result-company">{job.companyName}</p>

            <h3>
              <Link href={`/jobs/${encodeURIComponent(job.id)}`}>
                {job.title}
              </Link>
            </h3>
          </div>

          {job.freshness && (
            <span className="job-result-freshness">{job.freshness}</span>
          )}
        </div>

        <p className="job-result-location">
          {job.location ?? "Location not specified"}
        </p>

        <div className="job-result-meta">
          {job.workMode && <span>{job.workMode}</span>}

          {job.employmentType && <span>{job.employmentType}</span>}

          {job.postedDate && <span>{job.postedDate}</span>}

          {job.source && <span>{job.source}</span>}
        </div>

        {job.skills.length > 0 && (
          <div className="job-result-skills">
            {job.skills.slice(0, 5).map((skill) => (
              <span key={skill}>{skill}</span>
            ))}
          </div>
        )}
      </div>

      <Link
        className="job-result-link"
        href={`/jobs/${encodeURIComponent(job.id)}`}
      >
        View job →
      </Link>
    </article>
  );
}
