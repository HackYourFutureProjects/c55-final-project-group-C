type JobFiltersProps = {
  locations: string[];
  disciplines: string[];
  workModes: string[];
  employmentTypes: string[];
  searchQuery?: string;
  selectedLocation?: string;
  selectedDiscipline?: string;
  selectedWorkMode?: string;
  selectedEmploymentType?: string;
};

export default function JobFilters({
  locations,
  disciplines,
  workModes,
  employmentTypes,
  searchQuery = "",
  selectedLocation = "",
  selectedDiscipline = "",
  selectedWorkMode = "",
  selectedEmploymentType = "",
}: JobFiltersProps) {
  return (
    <form className="job-filters-form" action="/jobs">
      {searchQuery && <input type="hidden" name="q" value={searchQuery} />}

      <div className="job-filter-group">
        <label htmlFor="discipline">Discipline</label>

        <select
          id="discipline"
          name="discipline"
          defaultValue={selectedDiscipline}
        >
          <option value="">All disciplines</option>

          {disciplines.map((discipline) => (
            <option key={discipline} value={discipline}>
              {discipline}
            </option>
          ))}
        </select>
      </div>

      <div className="job-filter-group">
        <label htmlFor="work-mode">Work mode</label>

        <select id="work-mode" name="workMode" defaultValue={selectedWorkMode}>
          <option value="">Any work mode</option>

          {workModes.map((workMode) => (
            <option key={workMode} value={workMode}>
              {workMode}
            </option>
          ))}
        </select>
      </div>

      <div className="job-filter-group">
        <label htmlFor="location">Location</label>

        <select id="location" name="location" defaultValue={selectedLocation}>
          <option value="">Any location</option>

          {locations.map((location) => (
            <option key={location} value={location}>
              {location}
            </option>
          ))}
        </select>
      </div>

      <div className="job-filter-group">
        <label htmlFor="employment-type">Employment type</label>

        <select
          id="employment-type"
          name="employmentType"
          defaultValue={selectedEmploymentType}
        >
          <option value="">Any employment type</option>

          {employmentTypes.map((employmentType) => (
            <option key={employmentType} value={employmentType}>
              {employmentType}
            </option>
          ))}
        </select>
      </div>

      <button className="job-filters-submit" type="submit">
        Apply filters
      </button>
    </form>
  );
}
