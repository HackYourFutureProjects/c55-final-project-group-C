import Link from "next/link";

export default function TermsPage() {
  return (
    <main className="legal-page">
      <div className="legal-container">
        <p className="legal-eyebrow">LEGAL</p>

        <h1>Terms & Conditions</h1>

        <p className="legal-updated">
          Temporary draft — final legal copy will be added later.
        </p>

        <div className="legal-content">
          <section>
            <h2>Using JobMatch</h2>
            <p>
              JobMatch helps users discover job opportunities, understand job
              relevance, and organize their job search.
            </p>
          </section>

          <section>
            <h2>Your account</h2>
            <p>
              You are responsible for providing accurate account information and
              keeping access to your account secure.
            </p>
          </section>

          <section>
            <h2>Job information</h2>
            <p>
              Job listings may come from external sources. JobMatch does not
              guarantee that every listing is complete, current, or available.
            </p>
          </section>

          <section>
            <h2>External applications</h2>
            <p>
              Some application links may take you to third-party websites. Those
              websites operate under their own terms and policies.
            </p>
          </section>

          <section>
            <h2>No employment guarantee</h2>
            <p>
              Match information and recommendations are intended to support your
              job search and do not guarantee interviews, offers, or employment.
            </p>
          </section>

          <section>
            <h2>Changes to the service</h2>
            <p>
              JobMatch may change or improve its features as the service
              develops.
            </p>
          </section>
        </div>

        <Link className="legal-back-link" href="/register">
          Back to registration
        </Link>
      </div>
    </main>
  );
}
