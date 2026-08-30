export default function JobsLoading() {
  return (
    <main className="jobs-page">
      <div className="jobs-container">
        <header className="jobs-header">
          <p className="jobs-eyebrow">EXPLORE OPPORTUNITIES</p>

          <div className="jobs-heading-row">
            <div>
              <h1>Find jobs</h1>
              <p>Loading available opportunities…</p>
            </div>
          </div>
        </header>

        <section className="jobs-layout">
          <aside className="jobs-filters">
            <div className="jobs-loading-line jobs-loading-line-short" />
            <div className="jobs-loading-block" />
            <div className="jobs-loading-block" />
            <div className="jobs-loading-block" />
          </aside>

          <section className="jobs-results" aria-busy="true">
            <div className="jobs-loading-line jobs-loading-line-medium" />

            <div className="jobs-loading-result">
              <div className="jobs-loading-line jobs-loading-line-short" />
              <div className="jobs-loading-line jobs-loading-line-long" />
              <div className="jobs-loading-line jobs-loading-line-medium" />
            </div>

            <div className="jobs-loading-result">
              <div className="jobs-loading-line jobs-loading-line-short" />
              <div className="jobs-loading-line jobs-loading-line-long" />
              <div className="jobs-loading-line jobs-loading-line-medium" />
            </div>

            <div className="jobs-loading-result">
              <div className="jobs-loading-line jobs-loading-line-short" />
              <div className="jobs-loading-line jobs-loading-line-long" />
              <div className="jobs-loading-line jobs-loading-line-medium" />
            </div>
          </section>
        </section>
      </div>
    </main>
  );
}
