import Link from "next/link";
import { notFound } from "next/navigation";

type JobDetails = {
  id: string;
  title: string;
  companyName: string;
  location: string;
  workMode: string | null;
  employmentType: string | null;
  skills: string[];
  experienceLevel: string | null;
  educationLevel: string | null;
  salary: string | null;
  postedDate: string;
  freshness: string | null;
  source: string;
  description: string;
  sourceUrl: string;
};

const mockJob: JobDetails = {
  id: "test-job-123",
  title: "Frontend Developer",
  companyName: "Example Company",
  location: "Utrecht, Netherlands",
  workMode: "Hybrid",
  employmentType: "Full-time",
  skills: ["React", "TypeScript", "Next.js", "CSS"],
  experienceLevel: "Mid level",
  educationLevel: "Bachelor's degree or equivalent experience",
  salary: "€45,000 – €55,000 per year",
  postedDate: "Posted 2 days ago",
  freshness: "Fresh",
  source: "Company website",
  description:
    "We are looking for a Frontend Developer to join our product team. You will work on accessible, responsive web experiences and collaborate closely with design and backend engineers.",
  sourceUrl: "https://example.com/jobs/frontend-developer",
};

type JobDetailsPageProps = {
  params: Promise<{
    jobId: string;
  }>;
};

export default async function JobDetailsPage({ params }: JobDetailsPageProps) {
  const { jobId } = await params;
  if (jobId === "not-found") {
    notFound();
  }

  const job = {
    ...mockJob,
    id: jobId,
  };

  return (
    <main className="job-details-page">
      <div className="job-details-container">
        <Link className="job-details-back" href="/jobs">
          ← Back to jobs
        </Link>

        <section className="job-details-hero">
          <div>
            <p className="job-details-eyebrow">{job.companyName}</p>

            <h1>{job.title}</h1>

            <p className="job-details-location">{job.location}</p>
          </div>

          <div className="job-details-actions">
            <a
              className="job-details-apply"
              href={job.sourceUrl}
              target="_blank"
              rel="noreferrer"
            >
              Apply externally
            </a>
          </div>
        </section>

        <section className="job-details-meta" aria-label="Job information">
          <div>
            <span>Work mode</span>
            <strong>{job.workMode ?? "Not specified"}</strong>
          </div>

          <div>
            <span>Employment</span>
            <strong>{job.employmentType ?? "Not specified"}</strong>
          </div>

          <div>
            <span>Experience</span>
            <strong>{job.experienceLevel ?? "Not specified"}</strong>
          </div>

          <div>
            <span>Salary</span>
            <strong>{job.salary ?? "Not specified"}</strong>
          </div>
        </section>

        <div className="job-details-layout">
          <article className="job-details-main">
            <section>
              <p className="job-details-section-label">ABOUT THE ROLE</p>
              <h2>Job description</h2>

              <p className="job-details-description">{job.description}</p>
            </section>

            <section>
              <p className="job-details-section-label">SKILLS</p>
              <h2>What they are looking for</h2>

              {job.skills.length > 0 ? (
                <div className="job-details-skills">
                  {job.skills.map((skill) => (
                    <span key={skill}>{skill}</span>
                  ))}
                </div>
              ) : (
                <p>No skills specified.</p>
              )}
            </section>

            <section>
              <p className="job-details-section-label">REQUIREMENTS</p>
              <h2>Experience & education</h2>

              <dl className="job-details-requirements">
                <div>
                  <dt>Experience</dt>
                  <dd>{job.experienceLevel ?? "Not specified"}</dd>
                </div>

                <div>
                  <dt>Education</dt>
                  <dd>{job.educationLevel ?? "Not specified"}</dd>
                </div>
              </dl>
            </section>
          </article>

          <aside className="job-details-sidebar">
            <section>
              <p className="job-details-section-label">LISTING INFORMATION</p>

              <dl className="job-details-listing-info">
                <div>
                  <dt>Posted</dt>
                  <dd>{job.postedDate}</dd>
                </div>

                <div>
                  <dt>Freshness</dt>
                  <dd>{job.freshness ?? "Unknown"}</dd>
                </div>

                <div>
                  <dt>Source</dt>
                  <dd>{job.source}</dd>
                </div>
              </dl>
            </section>

            <section className="job-details-match-note">
              <p className="job-details-section-label">MATCH</p>
              <h2>Match information</h2>
              <p>
                Match details will appear here when the backend matching service
                is available.
              </p>
            </section>
          </aside>
        </div>
      </div>
    </main>
  );
}
