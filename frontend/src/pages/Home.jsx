import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';

function ShieldIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M12 3l7 2.5v5.8c0 4.8-2.9 8.2-7 9.7-4.1-1.5-7-4.9-7-9.7V5.5L12 3z" />
      <path d="M8.5 11.8l2.5 2.5 4.5-4.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export default function Home() {
  const [stats, setStats] = useState({ totalCertificates: 0, totalVerifications: 0, totalIssuers: 0 });

  useEffect(() => {
    let active = true;
    api
      .get('/public/stats')
      .then(({ data }) => {
        if (active) setStats(data);
      })
      .catch(() => {
        /* Stats are decorative; the page still works without them. */
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="home-page">
      <section className="hero">
        <div className="container hero-grid">
          <div className="hero-copy">
            <span className="pill">🔐 SHA-256 + RSA digital signatures</span>
            <h1>
              Verify any certificate
              <br />
              with <span className="text-gradient">complete confidence</span>
            </h1>
            <p className="hero-subtitle">
              CertiTrust lets institutions issue tamper-evident digital certificates and gives anyone
              — students, employers or admissions teams — an instant way to confirm authenticity.
              No account needed to verify.
            </p>
            <div className="hero-actions">
              <Link to="/verify" className="btn btn-primary btn-lg">
                Verify a certificate
              </Link>
              <Link to="/register" className="btn btn-outline btn-lg">
                Register your institution
              </Link>
            </div>
            <div className="hero-points">
              <span>✓ Upload, ID or QR verification</span>
              <span>✓ Instant VALID / TAMPERED status</span>
              <span>✓ Full audit trail</span>
            </div>
          </div>

          <div className="hero-visual" aria-hidden="true">
            <div className="hero-glow" />
            <div className="mock-certificate">
              <div className="mock-cert-band">
                <ShieldIcon />
                <span>Certificate of Completion</span>
              </div>
              <div className="mock-cert-body">
                <span className="mock-label">This is to certify that</span>
                <strong>Alice Johnson</strong>
                <span className="mock-course">B.Tech in Computer Science</span>
                <div className="mock-lines">
                  <span />
                  <span />
                </div>
                <div className="mock-footer">
                  <div>
                    <small>ISSUED</small>
                    <em>15 Jan 2026</em>
                  </div>
                  <div>
                    <small>CERTIFICATE ID</small>
                    <em>CERT-2026-DEMO01</em>
                  </div>
                  <div className="mock-qr">
                    {Array.from({ length: 25 }).map((_, index) => (
                      <span key={index} className={index % 3 === 0 ? 'filled' : ''} />
                    ))}
                  </div>
                </div>
              </div>
              <div className="mock-valid">
                <span>✓</span> VALID · Digitally signed
              </div>
            </div>
            <div className="floating-card floating-card-top">
              <ShieldIcon />
              <div>
                <strong>Tamper-evident</strong>
                <small>SHA-256 fingerprint</small>
              </div>
            </div>
            <div className="floating-card floating-card-bottom">
              <span className="floating-icon">QR</span>
              <div>
                <strong>Scan to verify</strong>
                <small>Works on any phone</small>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="stats-strip">
        <div className="container stats-grid">
          <div className="stat-block">
            <strong>{stats.totalCertificates.toLocaleString()}</strong>
            <span>Certificates issued</span>
          </div>
          <div className="stat-block">
            <strong>{stats.totalVerifications.toLocaleString()}</strong>
            <span>Verifications performed</span>
          </div>
          <div className="stat-block">
            <strong>{stats.totalIssuers.toLocaleString()}</strong>
            <span>Trusted institutions</span>
          </div>
          <div className="stat-block">
            <strong>3</strong>
            <span>Ways to verify</span>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="section-heading">
            <p className="eyebrow">Simple and secure</p>
            <h2>Three ways to verify a certificate</h2>
            <p>Every verification is checked against the stored hash and RSA signature — then logged.</p>
          </div>
          <div className="feature-grid">
            <article className="feature-card">
              <div className="feature-icon">
                <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" strokeWidth="1.7">
                  <path d="M12 16V4m0 0L8 8m4-4l4 4" strokeLinecap="round" strokeLinejoin="round" />
                  <path d="M4 15v3a2 2 0 002 2h12a2 2 0 002-2v-3" strokeLinecap="round" />
                </svg>
              </div>
              <h3>Upload the file</h3>
              <p>
                Upload the PDF, JPG or PNG certificate. We compute its SHA-256 hash and compare it with
                the issuer's stored fingerprint.
              </p>
              <Link to="/verify?tab=file" className="text-link">
                Verify by file →
              </Link>
            </article>

            <article className="feature-card">
              <div className="feature-icon">
                <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" strokeWidth="1.7">
                  <rect x="3" y="5" width="18" height="14" rx="2" />
                  <path d="M7 10h5M7 14h9" strokeLinecap="round" />
                </svg>
              </div>
              <h3>Enter the certificate ID</h3>
              <p>
                Type the unique ID printed on the certificate, such as CERT-2026-ABC123, to view its
                holder, course, dates and current status.
              </p>
              <Link to="/verify?tab=id" className="text-link">
                Verify by ID →
              </Link>
            </article>

            <article className="feature-card">
              <div className="feature-icon">
                <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" strokeWidth="1.7">
                  <rect x="3" y="3" width="7" height="7" rx="1" />
                  <rect x="14" y="3" width="7" height="7" rx="1" />
                  <rect x="3" y="14" width="7" height="7" rx="1" />
                  <path d="M14 14h3v3h-3zM20 14v3M14 20h6" strokeLinecap="round" />
                </svg>
              </div>
              <h3>Scan the QR code</h3>
              <p>
                Every issued certificate carries a QR code that opens the verification page directly —
                perfect for printed certificates.
              </p>
              <Link to="/verify?tab=qr" className="text-link">
                Verify by QR →
              </Link>
            </article>
          </div>
        </div>
      </section>

      <section className="section section-alt">
        <div className="container split-section">
          <div>
            <p className="eyebrow">Built for institutions</p>
            <h2>Issue credentials that can't be faked</h2>
            <p className="section-copy">
              Generate a professional certificate PDF with an embedded QR code, or upload an existing
              file. CertiTrust hashes and signs every certificate, stores an immutable verification
              record and gives you a complete audit trail.
            </p>
            <ul className="check-list">
              <li>Auto-generated PDF certificates with QR codes</li>
              <li>SHA-256 fingerprinting and RSA digital signatures</li>
              <li>Revoke certificates with a documented reason</li>
              <li>Approve institutions before they can issue</li>
              <li>Verification logs with time, IP and method</li>
            </ul>
            <Link to="/register" className="btn btn-primary">
              Become a trusted issuer
            </Link>
          </div>

          <div className="how-card">
            <h3>How verification works</h3>
            <ol className="steps">
              <li>
                <span>1</span>
                <div>
                  <strong>Submit a certificate</strong>
                  <p>Upload the file, enter an ID, or scan its QR code.</p>
                </div>
              </li>
              <li>
                <span>2</span>
                <div>
                  <strong>Fingerprint match</strong>
                  <p>The submitted file's SHA-256 hash is matched to the issued record.</p>
                </div>
              </li>
              <li>
                <span>3</span>
                <div>
                  <strong>Signature & status check</strong>
                  <p>The RSA signature, revocation and expiry status are validated.</p>
                </div>
              </li>
              <li>
                <span>4</span>
                <div>
                  <strong>Instant result</strong>
                  <p>See VALID, TAMPERED, REVOKED, EXPIRED or NOT FOUND — plus full details.</p>
                </div>
              </li>
            </ol>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container cta-banner">
          <div>
            <h2>Need to check a certificate right now?</h2>
            <p>No sign-up, no fees. Verification is open to everyone.</p>
          </div>
          <Link to="/verify" className="btn btn-white btn-lg">
            Verify certificate
          </Link>
        </div>
      </section>
    </div>
  );
}
