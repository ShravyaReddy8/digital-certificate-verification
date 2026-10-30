import { useEffect, useRef, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import api, { getErrorMessage } from '../api/client';
import CertificateDetails from '../components/CertificateDetails';
import StatusBadge from '../components/StatusBadge';
import { useToast } from '../context/ToastContext';
import { formatDateTime } from '../utils/format';

const TABS = [
  { key: 'file', label: 'Upload file' },
  { key: 'id', label: 'Certificate ID' },
  { key: 'qr', label: 'QR link' },
];

const RESULT_META = {
  VALID: {
    title: 'Certificate verified',
    icon: '✓',
    tone: 'valid',
    subtitle: 'The file matches the issuer’s fingerprint and the RSA signature is intact.',
  },
  TAMPERED: {
    title: 'Tampered / Not authentic',
    icon: '⚠',
    tone: 'invalid',
    subtitle: 'The certificate content does not match the record held by the issuer.',
  },
  REVOKED: {
    title: 'Certificate revoked',
    icon: '⛔',
    tone: 'invalid',
    subtitle: 'This certificate was withdrawn by the issuing institution.',
  },
  EXPIRED: {
    title: 'Certificate expired',
    icon: '⌛',
    tone: 'warning',
    subtitle: 'The certificate is authentic but its validity period has ended.',
  },
  NOT_FOUND: {
    title: 'Certificate not found',
    icon: '✕',
    tone: 'invalid',
    subtitle: 'No matching certificate exists in the registry.',
  },
};

function extractCertificateId(value) {
  const trimmed = value.trim();
  if (!trimmed) return '';
  const marker = '/verify/';
  if (trimmed.includes(marker)) {
    const after = trimmed.split(marker).pop() || '';
    return decodeURIComponent(after.split(/[?#]/)[0]).replace(/\/+$/, '');
  }
  return trimmed;
}

export default function Verify() {
  const { certificateId: routeId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialTab = TABS.some((tab) => tab.key === searchParams.get('tab'))
    ? searchParams.get('tab')
    : routeId
      ? 'id'
      : 'file';

  const [tab, setTab] = useState(initialTab);
  const [idInput, setIdInput] = useState(routeId || '');
  const [qrInput, setQrInput] = useState('');
  const [file, setFile] = useState(null);
  const [dragOver, setDragOver] = useState(false);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const fileInputRef = useRef(null);
  const autoVerified = useRef(null);
  const { addToast } = useToast();

  const selectTab = (key) => {
    setTab(key);
    setError('');
    setSearchParams(key === 'file' ? {} : { tab: key }, { replace: true });
  };

  const runVerification = async (request, description) => {
    setLoading(true);
    setError('');
    try {
      const { data } = await request();
      setResult(data);
      if (data.status === 'VALID') {
        addToast(`Verification passed for ${description}.`, 'success');
      } else {
        addToast(`Verification result: ${data.status.replace('_', ' ')}.`, 'info');
      }
    } catch (requestError) {
      const message = getErrorMessage(requestError);
      setError(message);
      addToast(message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const verifyById = (certificateId, method = 'CERTIFICATE_ID') =>
    runVerification(
      () => api.get(`/verify/${encodeURIComponent(certificateId)}`, { params: { method } }),
      certificateId,
    );

  useEffect(() => {
    if (!routeId) return;
    if (autoVerified.current === routeId) return;
    autoVerified.current = routeId;
    setIdInput(routeId);
    verifyById(routeId, 'QR');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [routeId]);

  const handleFileSubmit = (event) => {
    event.preventDefault();
    if (!file) {
      setError('Please choose a certificate file first.');
      return;
    }
    const formData = new FormData();
    formData.append('file', file);
    runVerification(() => api.post('/verify/file', formData), file.name);
  };

  const handleIdSubmit = (event) => {
    event.preventDefault();
    const id = extractCertificateId(idInput);
    if (!id) {
      setError('Enter a certificate ID to verify.');
      return;
    }
    verifyById(id);
  };

  const handleQrSubmit = (event) => {
    event.preventDefault();
    const id = extractCertificateId(qrInput);
    if (!id) {
      setError('Paste the QR verification link or the certificate ID.');
      return;
    }
    verifyById(id, 'QR');
  };

  const handleFileChange = (event) => {
    const selected = event.target.files?.[0] || null;
    if (selected && selected.size > 15 * 1024 * 1024) {
      setError('The file is larger than 15 MB.');
      setFile(null);
      return;
    }
    setError('');
    setFile(selected);
  };

  const handleDrop = (event) => {
    event.preventDefault();
    setDragOver(false);
    const dropped = event.dataTransfer.files?.[0];
    if (!dropped) return;
    if (!/\.(pdf|jpe?g|png)$/i.test(dropped.name)) {
      setError('Only PDF, JPG and PNG files are supported.');
      return;
    }
    setError('');
    setFile(dropped);
  };

  const meta = result ? RESULT_META[result.status] : null;

  return (
    <div className="page">
      <div className="container page-narrow">
        <div className="page-header">
          <p className="eyebrow">Public verification</p>
          <h1>Verify a certificate</h1>
          <p>
            Confirm that a certificate was genuinely issued and has not been altered. Verification is
            free and does not require an account.
          </p>
        </div>

        <div className="card verify-card">
          <div className="tabs" role="tablist" aria-label="Verification methods">
            {TABS.map((item) => (
              <button
                key={item.key}
                type="button"
                role="tab"
                aria-selected={tab === item.key}
                className={`tab ${tab === item.key ? 'active' : ''}`}
                onClick={() => selectTab(item.key)}
              >
                {item.label}
              </button>
            ))}
          </div>

          <div className="tab-panel">
            {tab === 'file' && (
              <form onSubmit={handleFileSubmit} className="verify-form">
                <div
                  className={`dropzone ${dragOver ? 'dragover' : ''} ${file ? 'has-file' : ''}`}
                  onDragOver={(event) => {
                    event.preventDefault();
                    setDragOver(true);
                  }}
                  onDragLeave={() => setDragOver(false)}
                  onDrop={handleDrop}
                  onClick={() => fileInputRef.current?.click()}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') fileInputRef.current?.click();
                  }}
                >
                  <div className="dropzone-icon">📄</div>
                  {file ? (
                    <>
                      <strong>{file.name}</strong>
                      <span>{(file.size / 1024).toFixed(1)} KB · click to choose another file</span>
                    </>
                  ) : (
                    <>
                      <strong>Drop your certificate here</strong>
                      <span>or click to browse — PDF, JPG, JPEG or PNG up to 15 MB</span>
                    </>
                  )}
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
                    onChange={handleFileChange}
                    hidden
                  />
                </div>
                <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
                  {loading ? <span className="spinner" /> : 'Verify uploaded certificate'}
                </button>
                <p className="form-hint">
                  The file stays on the server only long enough to compute its SHA-256 fingerprint.
                </p>
              </form>
            )}

            {tab === 'id' && (
              <form onSubmit={handleIdSubmit} className="verify-form">
                <label className="field">
                  <span className="field-label">Certificate ID</span>
                  <input
                    type="text"
                    placeholder="e.g. CERT-2026-DEMO01"
                    value={idInput}
                    onChange={(event) => setIdInput(event.target.value)}
                    autoComplete="off"
                  />
                </label>
                <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
                  {loading ? <span className="spinner" /> : 'Verify certificate ID'}
                </button>
                <p className="form-hint">
                  The certificate ID is printed on the certificate, usually in the bottom section.
                </p>
              </form>
            )}

            {tab === 'qr' && (
              <form onSubmit={handleQrSubmit} className="verify-form">
                <div className="qr-info">
                  <span className="qr-info-icon">▦</span>
                  <div>
                    <strong>Scan the QR code on the certificate</strong>
                    <p>
                      The QR code opens this page with the certificate ID filled in automatically. If
                      your camera app copied a link instead, paste it below.
                    </p>
                  </div>
                </div>
                <label className="field">
                  <span className="field-label">Verification link or certificate ID</span>
                  <input
                    type="text"
                    placeholder="http://localhost:5173/verify/CERT-2026-DEMO01"
                    value={qrInput}
                    onChange={(event) => setQrInput(event.target.value)}
                  />
                </label>
                <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
                  {loading ? <span className="spinner" /> : 'Verify QR link'}
                </button>
              </form>
            )}

            {error && <div className="alert alert-danger">{error}</div>}
          </div>
        </div>

        {result && meta && (
          <section className={`verify-result result-${meta.tone}`}>
            <div className="result-banner">
              <div className="result-icon" aria-hidden="true">
                {meta.icon}
              </div>
              <div className="result-copy">
                <div className="result-heading">
                  <h2>{meta.title}</h2>
                  <StatusBadge status={result.status} large />
                </div>
                <p className="result-message">{result.message}</p>
                <p className="result-meta">
                  Method: {result.method.replace('_', ' ')} · Checked {formatDateTime(result.verifiedAt)}
                </p>
              </div>
            </div>

            {result.certificate ? (
              <CertificateDetails certificate={result.certificate} />
            ) : (
              <div className="empty-result">
                <p>
                  Double-check the certificate ID or upload the original file issued by the
                  institution. If the result still fails, contact the issuer.
                </p>
              </div>
            )}

            {result.certificate && (
              <div className="qr-preview">
                <div>
                  <p className="eyebrow">Shareable verification</p>
                  <h3>QR code for this certificate</h3>
                  <p>Anyone can scan this code to see the same verification result.</p>
                  <code className="verify-link">{result.certificate.verificationUrl}</code>
                </div>
                <img
                  src={result.certificate.qrCodeUrl}
                  alt={`QR code for certificate ${result.certificate.certificateId}`}
                  width="140"
                  height="140"
                />
              </div>
            )}
          </section>
        )}

        {!result && (
          <div className="trust-note">
            <strong>How the check works:</strong> CertiTrust recomputes the SHA-256 fingerprint of the
            submitted file, validates the issuer's RSA signature over the stored fingerprint and checks
            revocation and expiry. Every attempt is recorded with its result, time and IP address.
          </div>
        )}
      </div>
    </div>
  );
}
