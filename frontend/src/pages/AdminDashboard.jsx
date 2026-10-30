import { useEffect, useState } from 'react';
import api, { getErrorMessage } from '../api/client';
import Modal from '../components/Modal';
import Pagination from '../components/Pagination';
import StatusBadge from '../components/StatusBadge';
import { useToast } from '../context/ToastContext';
import { formatDate, formatDateTime } from '../utils/format';

const TABS = [
  { key: 'overview', label: 'Overview' },
  { key: 'issuers', label: 'Issuer approvals' },
  { key: 'certificates', label: 'All certificates' },
  { key: 'logs', label: 'Verification logs' },
];

function StatCard({ label, value, tone, hint }) {
  return (
    <div className={`stat-card ${tone || ''}`}>
      <span className="stat-label">{label}</span>
      <strong className="stat-value">{value}</strong>
      {hint && <span className="stat-hint">{hint}</span>}
    </div>
  );
}

export default function AdminDashboard() {
  const { addToast } = useToast();
  const [tab, setTab] = useState('overview');
  const [refreshKey, setRefreshKey] = useState(0);

  const [dashboard, setDashboard] = useState(null);
  const [loadingDashboard, setLoadingDashboard] = useState(true);

  const [issuerStatus, setIssuerStatus] = useState('PENDING');
  const [issuerPage, setIssuerPage] = useState(0);
  const [issuers, setIssuers] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [loadingIssuers, setLoadingIssuers] = useState(false);
  const [rejectTarget, setRejectTarget] = useState(null);
  const [rejectReason, setRejectReason] = useState('');
  const [acting, setActing] = useState(false);

  const [certSearch, setCertSearch] = useState('');
  const [certAppliedSearch, setCertAppliedSearch] = useState('');
  const [certStatus, setCertStatus] = useState('ALL');
  const [certPage, setCertPage] = useState(0);
  const [certificates, setCertificates] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [loadingCerts, setLoadingCerts] = useState(false);

  const [logResult, setLogResult] = useState('ALL');
  const [logPage, setLogPage] = useState(0);
  const [logs, setLogs] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [loadingLogs, setLoadingLogs] = useState(false);

  useEffect(() => {
    let active = true;
    setLoadingDashboard(true);
    api
      .get('/admin/dashboard')
      .then(({ data }) => {
        if (active) setDashboard(data);
      })
      .catch((error) => {
        if (active) addToast(getErrorMessage(error), 'error');
      })
      .finally(() => {
        if (active) setLoadingDashboard(false);
      });
    return () => {
      active = false;
    };
  }, [refreshKey, addToast]);

  useEffect(() => {
    if (tab !== 'issuers') return undefined;
    let active = true;
    setLoadingIssuers(true);
    api
      .get('/admin/issuers', {
        params: { status: issuerStatus === 'ALL' ? undefined : issuerStatus, page: issuerPage, size: 8 },
      })
      .then(({ data }) => {
        if (active) setIssuers(data);
      })
      .catch((error) => {
        if (active) addToast(getErrorMessage(error), 'error');
      })
      .finally(() => {
        if (active) setLoadingIssuers(false);
      });
    return () => {
      active = false;
    };
  }, [tab, issuerStatus, issuerPage, refreshKey, addToast]);

  useEffect(() => {
    if (tab !== 'certificates') return undefined;
    let active = true;
    setLoadingCerts(true);
    api
      .get('/admin/certificates', {
        params: {
          search: certAppliedSearch || undefined,
          status: certStatus === 'ALL' ? undefined : certStatus,
          page: certPage,
          size: 8,
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
  }, [tab, certAppliedSearch, certStatus, certPage, refreshKey, addToast]);

  useEffect(() => {
    if (tab !== 'logs') return undefined;
    let active = true;
    setLoadingLogs(true);
    api
      .get('/admin/logs', {
        params: { result: logResult === 'ALL' ? undefined : logResult, page: logPage, size: 10 },
      })
      .then(({ data }) => {
        if (active) setLogs(data);
      })
      .catch((error) => {
        if (active) addToast(getErrorMessage(error), 'error');
      })
      .finally(() => {
        if (active) setLoadingLogs(false);
      });
    return () => {
      active = false;
    };
  }, [tab, logResult, logPage, refreshKey, addToast]);

  const updateIssuer = async (issuer, status, reason) => {
    setActing(true);
    try {
      await api.patch(`/admin/issuers/${issuer.id}/status`, { status, reason });
      addToast(
        status === 'APPROVED'
          ? `${issuer.institutionName || issuer.name} approved.`
          : `${issuer.institutionName || issuer.name} rejected.`,
        'success',
      );
      setRejectTarget(null);
      setRejectReason('');
      setRefreshKey((key) => key + 1);
    } catch (error) {
      addToast(getErrorMessage(error), 'error');
    } finally {
      setActing(false);
    }
  };

  const validRatio = dashboard
    ? Math.round(
        (dashboard.validVerifications /
          Math.max(1, dashboard.validVerifications + dashboard.invalidVerifications)) *
          100,
      )
    : 0;

  return (
    <div className="page dashboard-page">
      <div className="container">
        <div className="dashboard-header">
          <div>
            <p className="eyebrow">Administrator console</p>
            <h1>Registry overview</h1>
            <p>Approve institutions, audit every certificate and review verification activity.</p>
          </div>
          <button
            type="button"
            className="btn btn-outline"
            onClick={() => {
              setRefreshKey((key) => key + 1);
              addToast('Dashboard refreshed.', 'info');
            }}
          >
            Refresh data
          </button>
        </div>

        <div className="tabs dashboard-tabs">
          {TABS.map((item) => (
            <button
              key={item.key}
              type="button"
              className={`tab ${tab === item.key ? 'active' : ''}`}
              onClick={() => setTab(item.key)}
            >
              {item.label}
              {item.key === 'issuers' && dashboard?.pendingIssuers > 0 && (
                <span className="tab-badge">{dashboard.pendingIssuers}</span>
              )}
            </button>
          ))}
        </div>

        {tab === 'overview' && (
          <div className="overview-layout">
            {loadingDashboard && !dashboard ? (
              <div className="card table-loading">
                <span className="spinner" /> Loading dashboard…
              </div>
            ) : dashboard ? (
              <>
                <div className="stats-grid four">
                  <StatCard label="Total issuers" value={dashboard.totalIssuers} hint="Registered institutions" />
                  <StatCard label="Pending approval" value={dashboard.pendingIssuers} tone="warning" hint="Awaiting review" />
                  <StatCard label="Certificates" value={dashboard.totalCertificates} hint={`${dashboard.approvedIssuers} approved issuers`} />
                  <StatCard label="Verifications" value={dashboard.totalVerifications} hint="All-time checks" />
                  <StatCard label="Valid verifications" value={dashboard.validVerifications} tone="success" hint="Passed checks" />
                  <StatCard label="Failed verifications" value={dashboard.invalidVerifications} tone="danger" hint="Tampered, revoked, expired, missing" />
                  <StatCard label="Revoked certificates" value={dashboard.revokedCertificates} tone="danger" hint="Withdrawn by issuers" />
                  <StatCard label="Expired certificates" value={dashboard.expiredCertificates} tone="warning" hint="Past their expiry date" />
                </div>

                <div className="overview-grid">
                  <div className="card">
                    <div className="card-heading">
                      <div>
                        <h2>Verification outcome</h2>
                        <p>Share of verification attempts that passed.</p>
                      </div>
                    </div>
                    <div className="chart">
                      <div className="chart-row">
                        <span className="chart-label">Valid</span>
                        <div className="chart-track">
                          <div className="chart-bar bar-valid" style={{ width: `${validRatio}%` }} />
                        </div>
                        <span className="chart-value">{dashboard.validVerifications}</span>
                      </div>
                      <div className="chart-row">
                        <span className="chart-label">Not valid</span>
                        <div className="chart-track">
                          <div className="chart-bar bar-invalid" style={{ width: `${100 - validRatio}%` }} />
                        </div>
                        <span className="chart-value">{dashboard.invalidVerifications}</span>
                      </div>
                      <p className="chart-footnote">{validRatio}% of checks resulted in a VALID certificate.</p>
                    </div>
                    <div className="mini-stats">
                      <div>
                        <span>Certificates</span>
                        <strong>{dashboard.totalCertificates}</strong>
                      </div>
                      <div>
                        <span>Issuers</span>
                        <strong>{dashboard.approvedIssuers}</strong>
                      </div>
                      <div>
                        <span>Pending</span>
                        <strong>{dashboard.pendingIssuers}</strong>
                      </div>
                    </div>
                  </div>

                  <div className="card">
                    <div className="card-heading">
                      <div>
                        <h2>Recent verifications</h2>
                        <p>The latest checks performed through the public verifier.</p>
                      </div>
                      <button type="button" className="btn btn-ghost btn-sm" onClick={() => setTab('logs')}>
                        View all
                      </button>
                    </div>
                    {dashboard.recentVerifications.length === 0 ? (
                      <div className="table-empty">
                        <strong>No verifications yet</strong>
                        <p>Verification attempts will appear here as soon as they happen.</p>
                      </div>
                    ) : (
                      <ul className="activity-list">
                        {dashboard.recentVerifications.map((log) => (
                          <li key={log.id}>
                            <span className={`activity-dot dot-${log.result.toLowerCase()}`} />
                            <div className="activity-copy">
                              <strong>
                                {log.certificateId || 'Unknown certificate'}
                              </strong>
                              <span>
                                {log.method.replace('_', ' ')} · {formatDateTime(log.verifiedAt)}
                                {log.ipAddress ? ` · ${log.ipAddress}` : ''}
                              </span>
                            </div>
                            <StatusBadge status={log.result} />
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </div>
              </>
            ) : (
              <div className="card table-empty">
                <strong>Could not load dashboard</strong>
                <p>Check that the backend is running and refresh the page.</p>
              </div>
            )}
          </div>
        )}

        {tab === 'issuers' && (
          <div className="card">
            <div className="table-toolbar">
              <div className="card-heading inline">
                <div>
                  <h2>Issuer registrations</h2>
                  <p>Approve trusted institutions or reject applications with a reason.</p>
                </div>
              </div>
              <label className="inline-select">
                <span>Status</span>
                <select
                  value={issuerStatus}
                  onChange={(event) => {
                    setIssuerStatus(event.target.value);
                    setIssuerPage(0);
                  }}
                >
                  <option value="ALL">All</option>
                  <option value="PENDING">Pending</option>
                  <option value="APPROVED">Approved</option>
                  <option value="REJECTED">Rejected</option>
                </select>
              </label>
            </div>

            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Institution</th>
                    <th>Contact</th>
                    <th>Registered</th>
                    <th>Status</th>
                    <th className="align-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {loadingIssuers && (
                    <tr>
                      <td colSpan="5">
                        <div className="table-loading">
                          <span className="spinner" /> Loading issuers…
                        </div>
                      </td>
                    </tr>
                  )}
                  {!loadingIssuers && issuers.content.length === 0 && (
                    <tr>
                      <td colSpan="5">
                        <div className="table-empty">
                          <strong>No issuer accounts</strong>
                          <p>No registrations match this status filter.</p>
                        </div>
                      </td>
                    </tr>
                  )}
                  {!loadingIssuers &&
                    issuers.content.map((issuer) => (
                      <tr key={issuer.id}>
                        <td>
                          <strong>{issuer.institutionName || '—'}</strong>
                          <span className="table-sub">Account #{issuer.id}</span>
                        </td>
                        <td>
                          <span>{issuer.name}</span>
                          <span className="table-sub">{issuer.email}</span>
                          {issuer.rejectionReason && (
                            <span className="table-sub text-danger">
                              Reason: {issuer.rejectionReason}
                            </span>
                          )}
                        </td>
                        <td>{formatDate(issuer.createdAt)}</td>
                        <td>
                          <StatusBadge status={issuer.status} />
                        </td>
                        <td>
                          <div className="row-actions">
                            <button
                              type="button"
                              className="btn btn-success-outline btn-xs"
                              disabled={acting || issuer.status === 'APPROVED'}
                              onClick={() => updateIssuer(issuer, 'APPROVED')}
                            >
                              Approve
                            </button>
                            <button
                              type="button"
                              className="btn btn-danger-outline btn-xs"
                              disabled={acting || issuer.status === 'REJECTED'}
                              onClick={() => {
                                setRejectTarget(issuer);
                                setRejectReason('');
                              }}
                            >
                              Reject
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>

            <Pagination
              page={issuerPage}
              totalPages={issuers.totalPages}
              totalElements={issuers.totalElements}
              onPageChange={setIssuerPage}
            />
          </div>
        )}

        {tab === 'certificates' && (
          <div className="card">
            <div className="table-toolbar">
              <form
                className="search-form"
                onSubmit={(event) => {
                  event.preventDefault();
                  setCertPage(0);
                  setCertAppliedSearch(certSearch.trim());
                }}
              >
                <input
                  type="search"
                  placeholder="Search student, certificate ID, course, issuer"
                  value={certSearch}
                  onChange={(event) => setCertSearch(event.target.value)}
                  aria-label="Search all certificates"
                />
                <button type="submit" className="btn btn-outline btn-sm">
                  Search
                </button>
                {(certAppliedSearch || certSearch) && (
                  <button
                    type="button"
                    className="btn btn-ghost btn-sm"
                    onClick={() => {
                      setCertSearch('');
                      setCertAppliedSearch('');
                      setCertPage(0);
                    }}
                  >
                    Clear
                  </button>
                )}
              </form>
              <label className="inline-select">
                <span>Status</span>
                <select
                  value={certStatus}
                  onChange={(event) => {
                    setCertStatus(event.target.value);
                    setCertPage(0);
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
                    <th>Institution / issuer</th>
                    <th>Issued</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {loadingCerts && (
                    <tr>
                      <td colSpan="5">
                        <div className="table-loading">
                          <span className="spinner" /> Loading certificates…
                        </div>
                      </td>
                    </tr>
                  )}
                  {!loadingCerts && certificates.content.length === 0 && (
                    <tr>
                      <td colSpan="5">
                        <div className="table-empty">
                          <strong>No certificates found</strong>
                          <p>Try a different search term or status filter.</p>
                        </div>
                      </td>
                    </tr>
                  )}
                  {!loadingCerts &&
                    certificates.content.map((certificate) => (
                      <tr key={certificate.certificateId}>
                        <td>
                          <span className="mono table-id">{certificate.certificateId}</span>
                          <span className="table-sub">{certificate.courseTitle}</span>
                        </td>
                        <td>
                          <strong>{certificate.studentName}</strong>
                          <span className="table-sub">{certificate.studentEmail || '—'}</span>
                        </td>
                        <td>
                          <span>{certificate.institutionName}</span>
                          <span className="table-sub">{certificate.issuerEmail}</span>
                        </td>
                        <td>{formatDate(certificate.issueDate)}</td>
                        <td>
                          <StatusBadge status={certificate.status} />
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>

            <Pagination
              page={certPage}
              totalPages={certificates.totalPages}
              totalElements={certificates.totalElements}
              onPageChange={setCertPage}
            />
          </div>
        )}

        {tab === 'logs' && (
          <div className="card">
            <div className="table-toolbar">
              <div className="card-heading inline">
                <div>
                  <h2>Verification audit log</h2>
                  <p>Every public verification attempt with its method, result and origin.</p>
                </div>
              </div>
              <label className="inline-select">
                <span>Result</span>
                <select
                  value={logResult}
                  onChange={(event) => {
                    setLogResult(event.target.value);
                    setLogPage(0);
                  }}
                >
                  <option value="ALL">All results</option>
                  <option value="VALID">Valid</option>
                  <option value="TAMPERED">Tampered</option>
                  <option value="REVOKED">Revoked</option>
                  <option value="EXPIRED">Expired</option>
                  <option value="NOT_FOUND">Not found</option>
                </select>
              </label>
            </div>

            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>When</th>
                    <th>Certificate</th>
                    <th>Method</th>
                    <th>Result</th>
                    <th>IP address</th>
                  </tr>
                </thead>
                <tbody>
                  {loadingLogs && (
                    <tr>
                      <td colSpan="5">
                        <div className="table-loading">
                          <span className="spinner" /> Loading logs…
                        </div>
                      </td>
                    </tr>
                  )}
                  {!loadingLogs && logs.content.length === 0 && (
                    <tr>
                      <td colSpan="5">
                        <div className="table-empty">
                          <strong>No verification logs</strong>
                          <p>Attempts made through the public verify page will be logged here.</p>
                        </div>
                      </td>
                    </tr>
                  )}
                  {!loadingLogs &&
                    logs.content.map((log) => (
                      <tr key={log.id}>
                        <td>{formatDateTime(log.verifiedAt)}</td>
                        <td>
                          <span className="mono table-id">{log.certificateId || '—'}</span>
                          <span className="table-sub clamp-2">{log.message}</span>
                        </td>
                        <td>{log.method.replace('_', ' ')}</td>
                        <td>
                          <StatusBadge status={log.result} />
                        </td>
                        <td className="mono subtle">{log.ipAddress || '—'}</td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>

            <Pagination
              page={logPage}
              totalPages={logs.totalPages}
              totalElements={logs.totalElements}
              onPageChange={setLogPage}
            />
          </div>
        )}
      </div>

      {rejectTarget && (
        <Modal
          title={`Reject ${rejectTarget.institutionName || rejectTarget.name}?`}
          onClose={() => setRejectTarget(null)}
          footer={
            <>
              <button type="button" className="btn btn-ghost" onClick={() => setRejectTarget(null)}>
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-danger"
                onClick={() => updateIssuer(rejectTarget, 'REJECTED', rejectReason.trim())}
                disabled={acting || !rejectReason.trim()}
              >
                {acting ? <span className="spinner" /> : 'Reject registration'}
              </button>
            </>
          }
        >
          <p>
            The institution will not be able to sign in or issue certificates. This reason is recorded on
            the account.
          </p>
          <label className="field">
            <span className="field-label">Reason for rejection *</span>
            <textarea
              rows="4"
              value={rejectReason}
              onChange={(event) => setRejectReason(event.target.value)}
              placeholder="Explain why this institution cannot be approved…"
            />
          </label>
        </Modal>
      )}
    </div>
  );
}
