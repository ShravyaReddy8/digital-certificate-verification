const LABELS = {
  VALID: 'VALID',
  TAMPERED: 'TAMPERED',
  REVOKED: 'REVOKED',
  EXPIRED: 'EXPIRED',
  NOT_FOUND: 'NOT FOUND',
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
};

const CLASSES = {
  VALID: 'badge-success',
  APPROVED: 'badge-success',
  TAMPERED: 'badge-danger',
  REVOKED: 'badge-danger',
  NOT_FOUND: 'badge-danger',
  REJECTED: 'badge-danger',
  EXPIRED: 'badge-warning',
  PENDING: 'badge-warning',
};

export default function StatusBadge({ status, large = false }) {
  if (!status) return <span className="badge">UNKNOWN</span>;
  const normalized = String(status).toUpperCase();
  return (
    <span className={`badge ${CLASSES[normalized] || 'badge-neutral'} ${large ? 'badge-lg' : ''}`}>
      <span className="badge-dot" aria-hidden="true" />
      {LABELS[normalized] || normalized}
    </span>
  );
}
