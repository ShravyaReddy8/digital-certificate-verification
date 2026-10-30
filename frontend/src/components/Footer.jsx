import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-inner">
        <div>
          <div className="footer-brand">CertiTrust</div>
          <p>Secure, tamper-evident certificate verification powered by SHA-256 and RSA signatures.</p>
        </div>
        <div className="footer-links">
          <Link to="/">Home</Link>
          <Link to="/verify">Verify a certificate</Link>
          <Link to="/login">Issuer sign in</Link>
        </div>
      </div>
      <div className="footer-bottom">
        <div className="container">
          © {new Date().getFullYear()} CertiTrust · Digital Certificate Verification System
        </div>
      </div>
    </footer>
  );
}
