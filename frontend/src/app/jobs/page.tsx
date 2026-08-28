import JobFilters from "@/components/jobs/JobFilters";
import JobPagination from "@/components/jobs/JobPagination";
import JobResultItem from "@/components/jobs/JobResultItem";
import { getJobFilters } from "@/lib/api";
import { getMockJobs } from "@/lib/jobs";

type JobsPageProps = {
  searchParams: Promise<{
    q?: string;
    page?: string;
    discipline?: string;
    workMode?: string;
    location?: string;
    employmentType?: string;
  }>;
};

export default async function JobsPage({ searchParams }: JobsPageProps) {
  const { q, page, discipline, workMode, location, employmentType } =
    await searchParams;

  const searchQuery = q?.trim() ?? "";
  const currentPage = Math.max(Number(page) || 1, 1);

  const filters = await getJobFilters();

  const jobs = getMockJobs(searchQuery);
  const totalPages = 1;

  return (
    <main className="jobs-page">
      <div className="jobs-container">
        <header className="jobs-header">
          <p className="jobs-eyebrow">EXPLORE OPPORTUNITIES</p>

          <div className="jobs-heading-row">
            <div>
              <h1>Find jobs</h1>

              <p>
                Search fresh opportunities and focus on roles that are worth
                your time.
              </p>
            </div>
          </div>

          <form className="jobs-search" action="/jobs">
            {discipline && (
              <input type="hidden" name="discipline" value={discipline} />
            )}

            {workMode && (
              <input type="hidden" name="workMode" value={workMode} />
            )}

            {location && (
              <input type="hidden" name="location" value={location} />
            )}

            {employmentType && (
              <input
                type="hidden"
                name="employmentType"
                value={employmentType}
              />
            )}

            <label className="sr-only" htmlFor="jobs-search-input">
              Search by job title, keyword, or skill
            </label>

            <input
              id="jobs-search-input"
              name="q"
              type="search"
              defaultValue={searchQuery}
              placeholder="Job title, keyword, or skill"
            />

            <button type="submit">Search</button>
          </form>
        </header>

        <section className="jobs-layout">
          <aside className="jobs-filters">
            <div className="jobs-filters-header">
              <p className="jobs-section-label">FILTERS</p>
              <h2>Refine results</h2>
            </div>

            <JobFilters
              locations={filters.locations}
              disciplines={filters.disciplines}
              workModes={filters.workModes}
              employmentTypes={filters.employmentTypes}
              searchQuery={searchQuery}
              selectedLocation={location}
              selectedDiscipline={discipline}
              selectedWorkMode={workMode}
              selectedEmploymentType={employmentType}
            />
          </aside>

          <section className="jobs-results" aria-live="polite">
            <div className="jobs-results-header">
              <div>
                <p className="jobs-section-label">RESULTS</p>

                <h2>
                  {searchQuery
                    ? `Jobs matching “${searchQuery}”`
                    : "Explore available jobs"}
                </h2>
              </div>

              <p className="jobs-results-count">
                {jobs.length} {jobs.length === 1 ? "job" : "jobs"}
              </p>
            </div>

            {jobs.length > 0 ? (
              <div className="job-results-list">
                {jobs.map((job) => (
                  <JobResultItem key={job.id} job={job} />
                ))}
              </div>
            ) : (
              <div className="jobs-empty">
                <h3>No jobs found</h3>

                <p>
                  Try a different job title, keyword, or skill to broaden your
                  search.
                </p>
              </div>
            )}

            <JobPagination
              currentPage={currentPage}
              totalPages={totalPages}
              searchQuery={searchQuery}
            />
          </section>
        </section>
      </div>
    </main>
  );
}
