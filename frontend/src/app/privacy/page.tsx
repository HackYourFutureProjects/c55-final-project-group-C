import Link from "next/link";

export default function PrivacyPage() {
  return (
    <main className="legal-page">
      <div className="legal-container">
        <p className="legal-eyebrow">LEGAL</p>

        <h1>Privacy Policy</h1>

        <p className="legal-updated">
          Temporary draft — final legal copy will be added later.
        </p>

        <div className="legal-content">
          <section>
            <h2>Information we use</h2>
            <p>
              JobMatch may use information you provide, such as your name, email
              address, profile details, skills, preferences, and saved job
              activity.
            </p>
          </section>

          <section>
            <h2>Why we use this information</h2>
            <p>
              This information is used to provide account features, personalize
              your experience, support job matching, and help you organize your
              job search.
            </p>
          </section>

          <section>
            <h2>Authentication</h2>
            <p>
              JobMatch uses authenticated sessions to keep you signed in. The
              frontend does not store authentication tokens in localStorage.
            </p>
          </section>

          <section>
            <h2>External job sources</h2>
            <p>
              When you follow an external job or application link, the
              destination website may process information according to its own
              privacy policy.
            </p>
          </section>

          <section>
            <h2>Your account and data</h2>
            <p>
              You may be able to update or remove account information through
              the available account settings. Deleting your account permanently
              removes the account according to the current service behavior.
            </p>
          </section>

          <section>
            <h2>Changes to this policy</h2>
            <p>
              This policy may be updated as JobMatch develops and new features
              are introduced.
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
