import JobStatusSummary from "@/components/jobs/JobStatusSummary";
import { mockSavedJobs } from "@/lib/mocks/savedJobs";

export default function SavedJobsPage() {
  return (
    <main>
      <h1>Saved Jobs</h1>

      <JobStatusSummary jobs={mockSavedJobs} />
    </main>
  );
}
