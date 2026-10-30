export default function Pagination({ page, totalPages, totalElements, onPageChange }) {
  if (!totalPages || totalPages <= 1) {
    return totalElements > 0 ? (
      <div className="pagination">
        <span className="pagination-info">
          {totalElements} result{totalElements === 1 ? '' : 's'}
        </span>
      </div>
    ) : null;
  }

  return (
    <div className="pagination">
      <span className="pagination-info">
        Page {page + 1} of {totalPages} · {totalElements} result{totalElements === 1 ? '' : 's'}
      </span>
      <div className="pagination-buttons">
        <button
          type="button"
          className="btn btn-outline btn-sm"
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
        >
          Previous
        </button>
        <button
          type="button"
          className="btn btn-outline btn-sm"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}
