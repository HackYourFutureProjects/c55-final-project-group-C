import Link from "next/link";

type JobPaginationProps = {
  currentPage: number;
  totalPages: number;
  searchQuery: string;
};

function buildPageHref(page: number, searchQuery: string) {
  const params = new URLSearchParams();

  if (searchQuery) {
    params.set("q", searchQuery);
  }

  params.set("page", String(page));

  return `/jobs?${params.toString()}`;
}

export default function JobPagination({
  currentPage,
  totalPages,
  searchQuery,
}: JobPaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <nav className="job-pagination" aria-label="Job results pagination">
      {currentPage > 1 ? (
        <Link href={buildPageHref(currentPage - 1, searchQuery)}>
          ← Previous
        </Link>
      ) : (
        <span className="job-pagination-disabled">← Previous</span>
      )}

      <span className="job-pagination-status">
        Page {currentPage} of {totalPages}
      </span>

      {currentPage < totalPages ? (
        <Link href={buildPageHref(currentPage + 1, searchQuery)}>Next →</Link>
      ) : (
        <span className="job-pagination-disabled">Next →</span>
      )}
    </nav>
  );
}
