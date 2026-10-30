import { formatDate, formatDateTime } from '../utils/format';
import StatusBadge from './StatusBadge';

function Detail({ label, value, mono = false }) {
  return (
    <div className="detail-item">
      <span className="detail-label">{label}</span>
      <span className={`detail-value ${mono ? 'mono' : ''}`}>{value || '—'}</span>
    </div>
  );
}

export default function CertificateDetails({ certificate }) {
  if (!certificate) return null;

  return (
    <section className="certificate-card">
      <div className="certificate-card-head">
        <div>
          <p className="eyebrow">Certificate record</p>
          <h3>{certificate.studentName}</h3>
          <p className="certificate-course">{certificate.courseTitle}</p>
        </div>
        <StatusBadge status={certificate.status} large />
      </div>

      <div className="detail-grid">
        <Detail label="Certificate ID" value={certificate.certificateId} mono />
        <Detail label="Institution" value={certificate.institutionName} />
        <Detail label="Issued by" value={certificate.issuerName} />
        <Detail label="Student email" value={certificate.studentEmail} />
        <Detail label="Issue date" value={formatDate(certificate.issueDate)} />
        <Detail
          label="Valid until"
          value={certificate.expiryDate ? formatDate(certificate.expiryDate) : 'No expiry'}
        />
        <Detail label="Grade / remarks" value={certificate.grade} />
        <Detail label="Recorded on" value={formatDateTime(certificate.issuedAt)} />
      </div>

      {certificate.revoked && (
        <div className="alert alert-danger">
          <strong>Revoked certificate.</strong>{' '}
          {certificate.revocationReason || 'No reason was provided.'}
          {certificate.revokedAt && <> (revoked on {formatDateTime(certificate.revokedAt)})</>}
        </div>
      )}

      <div className="hash-block">
        <span className="detail-label">SHA-256 fingerprint</span>
        <code>{certificate.sha256Hash}</code>
      </div>
    </section>
  );
}
