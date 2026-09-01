import JobFilters from "@/components/jobs/JobFilters";
import JobResultsWithBookmarks from "@/components/jobs/JobResultsWithBookmarks";
import TopMatchesSection from "@/components/jobs/TopMatchesSection";
import { mapJobSearchResponse } from "@/lib/jobs";
import { getJobFiltersServer, getJobsServer } from "@/lib/jobs-server";

type SearchParamValue = string | string[] | undefined;

type JobsPageProps = {
  searchParams: Promise<{
    q?: SearchParamValue;
    discipline?: SearchParamValue;
    workMode?: SearchParamValue;
    location?: SearchParamValue;
  }>;
};

function getSingleSearchParam(value: SearchParamValue): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

export default async function JobsPage({ searchParams }: JobsPageProps) {
  const params = await searchParams;
  const q = getSingleSearchParam(params.q);
  const discipline = getSingleSearchParam(params.discipline);
  const workMode = getSingleSearchParam(params.workMode);
  const location = getSingleSearchParam(params.location);

  const searchQuery = q?.trim() ?? "";

  const [filters, jobResponse] = await Promise.all([
    getJobFiltersServer(),
    getJobsServer({
      discipline,
      workMode,
      location,
    }),
  ]);

  let jobs = jobResponse.map(mapJobSearchResponse);

  if (searchQuery) {
    const normalizedQuery = searchQuery.toLowerCase();

    // Backend does not expose a q parameter yet, so keyword search is limited
    // to the rows returned by the current backend filters.
    jobs = jobs.filter((job) => {
      const searchableText = [
        job.title,
        job.companyName,
        job.location,
        ...job.skills,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();

      return searchableText.includes(normalizedQuery);
    });
  }

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
              searchQuery={searchQuery}
              selectedLocation={location}
              selectedDiscipline={discipline}
              selectedWorkMode={workMode}
            />
          </aside>

          <section className="jobs-results" aria-live="polite">
            <TopMatchesSection />

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
              <JobResultsWithBookmarks jobs={jobs} />
            ) : (
              <div className="jobs-empty">
                <h3>No jobs found</h3>

                <p>
                  Try a different job title, keyword, or skill to broaden your
                  search.
                </p>
              </div>
            )}
          </section>
        </section>
      </div>
    </main>
  );
}
