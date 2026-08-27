export default function LoadingJobDetails() {
  return (
    <main className="job-details-page">
      <div className="job-details-container">
        <p className="job-details-state-eyebrow">LOADING</p>
        <h1 className="job-details-state-title">Loading job details...</h1>
        <p className="job-details-state-copy">
          We are getting the latest information for this job.
        </p>
      </div>
    </main>
  );
}
