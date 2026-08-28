export default function JobFilters() {
  return (
    <div className="job-filters-form">
      <div className="job-filter-group">
        <label htmlFor="discipline">Discipline</label>

        <select id="discipline" name="discipline" disabled>
          <option>All disciplines</option>
        </select>
      </div>

      <div className="job-filter-group">
        <label htmlFor="work-mode">Work mode</label>

        <select id="work-mode" name="workMode" disabled>
          <option>Any work mode</option>
        </select>
      </div>

      <div className="job-filter-group">
        <label htmlFor="employment-type">Employment type</label>

        <select id="employment-type" name="employmentType" disabled>
          <option>Any employment type</option>
        </select>
      </div>

      <div className="job-filter-group">
        <label htmlFor="posted-date">Posted</label>

        <select id="posted-date" name="postedDate" disabled>
          <option>Any time</option>
        </select>
      </div>

      <p className="job-filter-status">
        Filters will become active when the backend filter endpoint is ready.
      </p>
    </div>
  );
}
