import Link from "next/link";

export default function Home() {
  return (
    <>
      <section className="hero-section">
        <div className="hero-content">
          <p className="hero-eyebrow">JOB SEARCH, WITH MORE CLARITY</p>

          <h1 className="hero-title">
            Find work
            <br />
            that actually
            <br />
            fits you.
          </h1>

          <p className="hero-description">
            Search relevant and fresh jobs, understand your match, and spend
            less time on outdated listings.
          </p>

          <form className="hero-search" action="/jobs">
            <label className="sr-only" htmlFor="job-search">
              Search by job title, keyword, or skill
            </label>

            <input
              id="job-search"
              name="q"
              type="search"
              placeholder="Job title, keyword, or skill"
            />

            <button type="submit">Find jobs</button>
          </form>

          <div className="hero-actions">
            <Link className="primary-link" href="/jobs">
              Explore jobs
            </Link>

            <Link className="secondary-link" href="/register">
              Create your profile
            </Link>
          </div>
        </div>

        <section className="hero-panel" aria-label="JobMatch product preview">
          <div className="preview-label">TODAY&apos;S MATCH</div>

          <div className="preview-score">86%</div>

          <p className="preview-title">Frontend Developer</p>
          <p className="preview-company">Amsterdam · Hybrid</p>

          <div className="preview-divider" />

          <div className="preview-row">
            <span>Skills matched</span>
            <strong>7 of 9</strong>
          </div>

          <div className="preview-row">
            <span>Listing signal</span>
            <strong>Fresh</strong>
          </div>

          <p className="preview-note">
            You match most of the required frontend skills. Two skills are
            missing.
          </p>
        </section>
      </section>

      <section className="value-section">
        <article>
          <span>01</span>
          <h2>Relevant jobs</h2>
          <p>
            Focus on opportunities that better match your skills, preferences,
            and experience.
          </p>
        </article>

        <article>
          <span>02</span>
          <h2>Transparent matching</h2>
          <p>
            See why a job matches you and which important skills may still be
            missing.
          </p>
        </article>

        <article>
          <span>03</span>
          <h2>Fresh listings</h2>
          <p>
            Understand whether a listing is recent and worth your time before
            you apply.
          </p>
        </article>
      </section>
    </>
  );
}
