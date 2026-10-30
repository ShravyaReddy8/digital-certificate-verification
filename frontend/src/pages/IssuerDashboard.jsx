import { useCallback, useEffect, useState } from 'react';
import api, { downloadFile, getErrorMessage } from '../api/client';
import Modal from '../components/Modal';
import Pagination from '../components/Pagination';
import StatusBadge from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { formatDate, todayIso } from '../utils/format';

const EMPTY_ISSUE_FORM = {
  studentName: '',
  studentEmail: '',
  courseTitle: '',
  institutionName: '',
  issueDate: '',
  expiryDate: '',
  grade: '',
};

function StatCard({ label, value, tone, hint }) {
  return (
    <div className={`stat-card ${tone || ''}`}>
      <span className="stat-label">{label}</span>
      <strong className="stat-value">{value}</strong>
      {hint && <span className="stat-hint">{hint}</span>}
    </div>
  );
}

export default function IssuerDashboard() {
  const { user } = useAuth();
  const { addToast } = useToast();

  const [tab, setTab] = useState('certificates');
  const [stats, setStats] = useState({ totalCertificates: 0, validCertificates: 0, revokedCertificates: 0, expiredCertificates: 0 });
  const [refreshKey, setRefreshKey] = useState(0);

  const [search, setSearch] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [page, setPage] = useState(0);
  const [certificates, setCertificates] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0 });
  const [loadingCerts, setLoadingCerts] = useState(true);

  const [issueMode, setIssueMode] = useState('generate');
  const [form, setForm] = useState({
    ...EMPTY_ISSUE_FORM,
    institutionName: user?.institutionName || '',
    issueDate: todayIso(),
  });
  const [file, setFile] = useState(null);
  const [formErrors, setFormErrors] = useState({});
  const [issuing, setIssuing] = useState(false);

  const [revokeTarget, setRevokeTarget] = useState(null);
  const [revokeReason, setRevokeReason] = useState('');
  const [revoking, setRevoking] = useState(false);
  const [downloading, setDownloading] = useState('');

  useEffect(() => {
    if (user?.institutionName) {
      setForm((current) => ({ ...current, institutionName: user.institutionName }));
    }
  }, [user]);

  useEffect(() => {
    let active = true;
    api
      .get('/issuer/stats')
      .then(({ data }) => {
        if (active) setStats(data);
      })
      .catch(() => {
        /* dashboard still works without the optional stats strip */
      });
    return () => {
      active = false;
    };
  }, [refreshKey]);

  useEffect(() => {
    let active = true;
    setLoadingCerts(true);
    api
      .get('/issuer/certificates', {
        params: {
          page,
          size: 8,
          search: appliedSearch || undefined,
          status: statusFilter === 'ALL' ? undefined : statusFilter,
        },
      })
      .then(({ data }) => {
        if (active) setCertificates(data);
      })
      .catch((error) => {
        if (active) addToast(getErrorMessage(error), 'error');
      })
      .finally(() => {
        if (active) setLoadingCerts(false);
      });
    return () => {
      active = false;
    };
  }, [page, appliedSearch, statusFilter, refreshKey, addToast]);

  const applySearch = (event) => {
    event.preventDefault();
    setPage(0);
    setAppliedSearch(search.trim());
  };

  const resetIssueForm = () => {
    setForm({
      ...EMPTY_ISSUE_FORM,
      institutionName: user?.institutionName || '',
      issueDate: todayIso(),
    });
    setFile(null);
    setFormErrors({});
  };

  const handleFormChange = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
    setFormErrors((current) => ({ ...current, [event.target.name]: undefined }));
  };

  const validateIssueForm = () => {
    const errors = {};
    if (!form.studentName.trim()) errors.studentName = 'Student name is required.';
    if (form.studentEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.studentEmail)) {
      errors.studentEmail = 'Enter a valid email address.';
    }
    if (!form.courseTitle.trim()) errors.courseTitle = 'Course or title is required.';
    if (!form.institutionName.trim()) errors.institutionName = 'Institution name is required.';
    if (!form.issueDate) errors.issueDate = 'Issue date is required.';
    else if (form.issueDate > todayIso()) errors.issueDate = 'Issue date cannot be in the future.';
    if (form.expiryDate && form.issueDate && form.expiryDate < form.issueDate) {
      errors.expiryDate = 'Expiry must be on or after the issue date.';
    }
    if (issueMode === 'upload' && !file) errors.file = 'Choose a PDF, JPG or PNG file.';
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleIssue = async (event) => {
    event.preventDefault();
    if (!validateIssueForm()) return;
    setIssuing(true);
    try {
      let response;
      if (issueMode === 'upload') {
        const payload = new FormData();
        Object.entries(form).forEach(([key, value]) => {
          if (value) payload.append(key, value);
        });
        payload.append('file', file);
        response = await api.post('/issuer/certificates/upload', payload);
      } else {
        const payload = {
          studentName: form.studentName.trim(),
          studentEmail: form.studentEmail.trim() || null,
          courseTitle: form.courseTitle.trim(),
          institutionName: form.institutionName.trim(),
          issueDate: form.issueDate,
          expiryDate: form.expiryDate || null,
          grade: form.grade.trim() || null,
        };
        response = await api.post('/issuer/certificates', payload);
      }
      addToast(`Certificate ${response.data.certificateId} issued successfully.`, 'success');
      resetIssueForm();
      setPage(0);
      setAppliedSearch('');
      setSearch('');
      setStatusFilter('ALL');
      setRefreshKey((key) => key + 1);
      setTab('certificates');
    } catch (error) {
      addToast(getErrorMessage(error), 'error');
    } finally {
      setIssuing(false);
    }
  };

  const handleDownload = async (certificate, type) => {
    setDownloading(`${certificate.certificateId}-${type}`);
    try {
      if (type === 'qr') {
        await downloadFile(
          `/issuer/certificates/${certificate.certificateId}/qr`,
          `qr-${certificate.certificateId}.png`,
        );
      } else {
        await downloadFile(
          `/issuer/certificates/${certificate.certificateId}/download`,
          certificate.fileName,
        );
      }
    } catch (error) {
      addToast(getErrorMessage(error), 'error');
    } finally {
      setDownloading('');
    }
  };

  const openRevoke = (certificate) => {
    setRevokeTarget(certificate);
    setRevokeReason('');
  };

  const confirmRevoke = async () => {
    if (!revokeReason.trim()) return;
    setRevoking(true);
    try {
      await api.post(`/issuer/certificates/${revokeTarget.certificateId}/revoke`, {
        reason: revokeReason.trim(),
      });
      addToast(`Certificate ${revokeTarget.certificateId} revoked.`, 'success');
      setRevokeTarget(null);
      setRefreshKey((key) => key + 1);
    } catch (error) {
      addToast(getErrorMessage(error), 'error');
    } finally {
      setRevoking(false);
    }
  };

  const canRevoke = useCallback((certificate) => !certificate.revoked, []);

  return (
    <div className="page dashboard-page">
      <div className="container">
        <div className="dashboard-header">
          <div>
            <p className="eyebrow">Issuer workspace</p>
            <h1>Welcome, {user?.name}</h1>
            <p>
              {user?.institutionName} · Issue, track and revoke your institution's certificates.
            </p>
          </div>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => setTab(tab === 'issue' ? 'certificates' : 'issue')}
          >
            {tab === 'issue' ? 'View my certificates' : '+ Issue certificate'}
          </button>
        </div>

        <div className="stats-grid four">
          <StatCard label="Total certificates" value={stats.totalCertificates} hint="Issued by you" />
          <StatCard label="Valid" value={stats.validCertificates} tone="success" hint="Current and active" />
          <StatCard label="Revoked" value={stats.revokedCertificates} tone="danger" hint="Withdrawn credentials" />
          <StatCard label="Expired" value={stats.expiredCertificates} tone="warning" hint="Past expiry date" />
        </div>

        <div className="tabs dashboard-tabs">
          <button
            type="button"
            className={`tab ${tab === 'certificates' ? 'active' : ''}`}
            onClick={() => setTab('certificates')}
          >
            My certificates
          </button>
          <button
            type="button"
            className={`tab ${tab === 'issue' ? 'active' : ''}`}
            onClick={() => setTab('issue')}
          >
            Issue certificate
          </button>
        </div>

        {tab === 'certificates' && (
          <div className="card">
            <div className="table-toolbar">
              <form className="search-form" onSubmit={applySearch}>
                <input
                  type="search"
                  placeholder="Search by student, course or certificate ID"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  aria-label="Search certificates"
                />
                <button type="submit" className="btn btn-outline btn-sm">
                  Search
                </button>
                {(appliedSearch || search) && (
                  <button
                    type="button"
                    className="btn btn-ghost btn-sm"
                    onClick={() => {
                      setSearch('');
                      setAppliedSearch('');
                      setPage(0);
                    }}
                  >
                    Clear
                  </button>
                )}
              </form>
              <label className="inline-select">
                <span>Status</span>
                <select
                  value={statusFilter}
                  onChange={(event) => {
                    setStatusFilter(event.target.value);
                    setPage(0);
                  }}
                >
                  <option value="ALL">All</option>
                  <option value="VALID">Valid</option>
                  <option value="REVOKED">Revoked</option>
                  <option value="EXPIRED">Expired</option>
                </select>
              </label>
            </div>

            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Certificate</th>
                    <th>Student</th>
                    <th>Course</th>
                    <th>Issued</th>
                    <th>Status</th>
                    <th className="align-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {loadingCerts && (
                    <tr>
                      <td colSpan="6">
                        <div className="table-loading">
                          <span className="spinner" /> Loading certificates…
                        </div>
                      </td>
                    </tr>
                  )}
                  {!loadingCerts && certificates.content.length === 0 && (
                    <tr>
                      <td colSpan="6">
                        <div className="table-empty">
                          <strong>No certificates found</strong>
                          <p>
                            {appliedSearch || statusFilter !== 'ALL'
                              ? 'Try a different search or status filter.'
                              : 'Issue your first certificate to get started.'}
                          </p>
                          <button type="button" className="btn btn-primary btn-sm" onClick={() => setTab('issue')}>
                            Issue certificate
                          </button>
                        </div>
                      </td>
                    </tr>
                  )}
                  {!loadingCerts &&
                    certificates.content.map((certificate) => (
                      <tr key={certificate.certificateId}>
                        <td>
                          <span className="mono table-id">{certificate.certificateId}</span>
                          <span className="table-sub">{certificate.fileName}</span>
                        </td>
                        <td>
                          <strong>{certificate.studentName}</strong>
                          <span className="table-sub">{certificate.studentEmail || 'No email'}</span>
                        </td>
                        <td>
                          <span className="clamp-2">{certificate.courseTitle}</span>
                          <span className="table-sub">{certificate.grade ? `Grade: ${certificate.grade}` : ''}</span>
                        </td>
                        <td>{formatDate(certificate.issueDate)}</td>
                        <td>
                          <StatusBadge status={certificate.status} />
                        </td>
                        <td>
                          <div className="row-actions">
                            <button
                              type="button"
                              className="btn btn-outline btn-xs"
                              onClick={() => handleDownload(certificate, 'file')}
                              disabled={downloading === `${certificate.certificateId}-file`}
                            >
                              {downloading === `${certificate.certificateId}-file` ? '…' : 'Download'}
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline btn-xs"
                              onClick={() => handleDownload(certificate, 'qr')}
                              disabled={downloading === `${certificate.certificateId}-qr`}
                            >
                              QR
                            </button>
                            {canRevoke(certificate) && (
                              <button
                                type="button"
                                className="btn btn-danger-outline btn-xs"
                                onClick={() => openRevoke(certificate)}
                              >
                                Revoke
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>

            <Pagination
              page={certificates.page}
              totalPages={certificates.totalPages}
              totalElements={certificates.totalElements}
              onPageChange={setPage}
            />
          </div>
        )}

        {tab === 'issue' && (
          <div className="issue-layout">
            <div className="card">
              <div className="card-heading">
                <div>
                  <h2>Issue a new certificate</h2>
                  <p>
                    {issueMode === 'generate'
                      ? 'We generate a professional PDF with an embedded verification QR code.'
                      : 'Upload an existing PDF, JPG or PNG file to register it on-chain-free and sign it.'}
                  </p>
                </div>
              </div>

              <div className="mode-switch" role="group" aria-label="Certificate source">
                <button
                  type="button"
                  className={issueMode === 'generate' ? 'active' : ''}
                  onClick={() => setIssueMode('generate')}
                >
                  Auto-generate PDF
                </button>
                <button
                  type="button"
                  className={issueMode === 'upload' ? 'active' : ''}
                  onClick={() => setIssueMode('upload')}
                >
                  Upload existing file
                </button>
              </div>

              <form onSubmit={handleIssue} className="stack-form" noValidate>
                <div className="field-row">
                  <label className="field">
                    <span className="field-label">Student full name *</span>
                    <input
                      type="text"
                      name="studentName"
                      value={form.studentName}
                      onChange={handleFormChange}
                      placeholder="e.g. Alice Johnson"
                      className={formErrors.studentName ? 'invalid' : ''}
                    />
                    {formErrors.studentName && (
                      <span className="field-error">{formErrors.studentName}</span>
                    )}
                  </label>
                  <label className="field">
                    <span className="field-label">Student email</span>
                    <input
                      type="email"
                      name="studentEmail"
                      value={form.studentEmail}
                      onChange={handleFormChange}
                      placeholder="student@example.com"
                      className={formErrors.studentEmail ? 'invalid' : ''}
                    />
                    {formErrors.studentEmail && (
                      <span className="field-error">{formErrors.studentEmail}</span>
                    )}
                  </label>
                </div>

                <label className="field">
                  <span className="field-label">Course / certificate title *</span>
                  <input
                    type="text"
                    name="courseTitle"
                    value={form.courseTitle}
                    onChange={handleFormChange}
                    placeholder="e.g. B.Tech in Computer Science"
                    className={formErrors.courseTitle ? 'invalid' : ''}
                  />
                  {formErrors.courseTitle && (
                    <span className="field-error">{formErrors.courseTitle}</span>
                  )}
                </label>

                <div className="field-row">
                  <label className="field">
                    <span className="field-label">Institution name *</span>
                    <input
                      type="text"
                      name="institutionName"
                      value={form.institutionName}
                      onChange={handleFormChange}
                      className={formErrors.institutionName ? 'invalid' : ''}
                    />
                    {formErrors.institutionName && (
                      <span className="field-error">{formErrors.institutionName}</span>
                    )}
                  </label>
                  <label className="field">
                    <span className="field-label">Grade / remarks</span>
                    <input
                      type="text"
                      name="grade"
                      value={form.grade}
                      onChange={handleFormChange}
                      placeholder="e.g. A+ / Distinction"
                    />
                  </label>
                </div>

                <div className="field-row">
                  <label className="field">
                    <span className="field-label">Issue date *</span>
                    <input
                      type="date"
                      name="issueDate"
                      value={form.issueDate}
                      max={todayIso()}
                      onChange={handleFormChange}
                      className={formErrors.issueDate ? 'invalid' : ''}
                    />
                    {formErrors.issueDate && (
                      <span className="field-error">{formErrors.issueDate}</span>
                    )}
                  </label>
                  <label className="field">
                    <span className="field-label">Expiry date (optional)</span>
                    <input
                      type="date"
                      name="expiryDate"
                      value={form.expiryDate}
                      min={form.issueDate || undefined}
                      onChange={handleFormChange}
                      className={formErrors.expiryDate ? 'invalid' : ''}
                    />
                    {formErrors.expiryDate ? (
                      <span className="field-error">{formErrors.expiryDate}</span>
                    ) : (
                      <span className="field-hint">Leave blank for a certificate with no expiry.</span>
                    )}
                  </label>
                </div>

                {issueMode === 'upload' && (
                  <label className={`file-field ${formErrors.file ? 'invalid' : ''}`}>
                    <span className="field-label">Certificate file *</span>
                    <input
                      type="file"
                      accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
                      onChange={(event) => {
                        setFile(event.target.files?.[0] || null);
                        setFormErrors((current) => ({ ...current, file: undefined }));
                      }}
                    />
                    <span className="field-hint">
                      PDF, JPG, JPEG or PNG up to 15 MB. The file is hashed and RSA-signed.
                    </span>
                    {formErrors.file && <span className="field-error">{formErrors.file}</span>}
                  </label>
                )}

                <div className="form-actions">
                  <button type="button" className="btn btn-ghost" onClick={resetIssueForm}>
                    Reset
                  </button>
                  <button type="submit" className="btn btn-primary" disabled={issuing}>
                    {issuing ? <span className="spinner" /> : 'Issue certificate'}
                  </button>
                </div>
              </form>
            </div>

            <aside className="issue-aside">
              <div className="info-card">
                <h3>What happens on issue?</h3>
                <ul className="steps compact">
                  <li>
                    <span>1</span>
                    <div>
                      <strong>Unique ID</strong>
                      <p>A CERT-YYYY-XXXXXX identifier is generated.</p>
                    </div>
                  </li>
                  <li>
                    <span>2</span>
                    <div>
                      <strong>SHA-256 fingerprint</strong>
                      <p>The exact bytes of the certificate are hashed.</p>
                    </div>
                  </li>
                  <li>
                    <span>3</span>
                    <div>
                      <strong>RSA signature</strong>
                      <p>The hash is signed with the server's private key.</p>
                    </div>
                  </li>
                  <li>
                    <span>4</span>
                    <div>
                      <strong>QR verification</strong>
                      <p>A QR code links straight to this certificate's public page.</p>
                    </div>
                  </li>
                </ul>
              </div>
              <div className="info-card tinted">
                <h3>Good to know</h3>
                <p>
                  Revoking a certificate keeps it in the registry but marks it as REVOKED for all future
                  verification attempts. Downloads include the QR code so printed copies stay verifiable.
                </p>
              </div>
            </aside>
          </div>
        )}
      </div>

      {revokeTarget && (
        <Modal
          title={`Revoke ${revokeTarget.certificateId}?`}
          onClose={() => setRevokeTarget(null)}
          footer={
            <>
              <button type="button" className="btn btn-ghost" onClick={() => setRevokeTarget(null)}>
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-danger"
                onClick={confirmRevoke}
                disabled={revoking || !revokeReason.trim()}
              >
                {revoking ? <span className="spinner" /> : 'Revoke certificate'}
              </button>
            </>
          }
        >
          <p>
            Revoking <strong>{revokeTarget.studentName}'s</strong> certificate is permanent. Anyone who
            verifies it afterwards will see a REVOKED result.
          </p>
          <label className="field">
            <span className="field-label">Reason for revocation *</span>
            <textarea
              rows="4"
              value={revokeReason}
              onChange={(event) => setRevokeReason(event.target.value)}
              placeholder="Explain why this certificate is being revoked…"
            />
            <span className="field-hint">This reason is shown to verifiers.</span>
          </label>
        </Modal>
      )}
    </div>
  );
}
