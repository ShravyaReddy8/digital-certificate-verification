import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="page">
      <div className="container page-narrow not-found">
        <span className="not-found-code">404</span>
        <h1>Page not found</h1>
        <p>The page you are looking for does not exist or may have moved.</p>
        <div className="hero-actions centered">
          <Link to="/" className="btn btn-primary">
            Back to home
          </Link>
          <Link to="/verify" className="btn btn-outline">
            Verify a certificate
          </Link>
        </div>
      </div>
    </div>
  );
}
