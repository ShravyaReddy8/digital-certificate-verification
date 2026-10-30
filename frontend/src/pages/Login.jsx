import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { getErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

const DEMO_ACCOUNTS = [
  { label: 'Issuer', email: 'issuer@cert.com', password: 'Issuer@123' },
  { label: 'Admin', email: 'admin@cert.com', password: 'Admin@123' },
];

export default function Login() {
  const { login, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { addToast } = useToast();

  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const destinationFor = (role) => (role === 'ADMIN' ? '/admin' : '/issuer');

  useEffect(() => {
    if (user) {
      navigate(destinationFor(user.role), { replace: true });
    }
  }, [user, navigate]);

  const handleChange = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
    setError('');
  };

  const fillDemo = (account) => {
    setForm({ email: account.email, password: account.password });
    setError('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!form.email.trim() || !form.password) {
      setError('Please enter your email and password.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const loggedIn = await login(form.email.trim(), form.password);
      addToast(`Welcome back, ${loggedIn.name}.`, 'success');
      const redirectTo = location.state?.from;
      navigate(redirectTo || destinationFor(loggedIn.role), { replace: true });
    } catch (loginError) {
      setError(getErrorMessage(loginError));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page auth-page">
      <div className="container auth-grid">
        <div className="auth-aside">
          <p className="eyebrow">Secure access</p>
          <h1>Sign in to your dashboard</h1>
          <p>
            Issuers manage certificate issuance and revocation. Administrators approve institutions and
            monitor every verification.
          </p>
          <ul className="check-list">
            <li>JWT-secured, role-based access</li>
            <li>Only approved institutions can issue</li>
            <li>Complete audit history of verifications</li>
          </ul>
          <div className="auth-art">
            <span className="auth-art-shield">✓</span>
            <div>
              <strong>Your credentials are protected</strong>
              <small>Passwords are hashed with BCrypt; sessions use short-lived JWTs.</small>
            </div>
          </div>
        </div>

        <div className="card auth-card">
          <h2>Welcome back</h2>
          <p className="card-subtitle">Sign in with your registered account.</p>

          {error && <div className="alert alert-danger">{error}</div>}

          <form onSubmit={handleSubmit} className="stack-form" noValidate>
            <label className="field">
              <span className="field-label">Email address</span>
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                placeholder="you@institution.edu"
                autoComplete="email"
              />
            </label>
            <label className="field">
              <span className="field-label">Password</span>
              <input
                type="password"
                name="password"
                value={form.password}
                onChange={handleChange}
                placeholder="Your password"
                autoComplete="current-password"
              />
            </label>
            <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
              {submitting ? <span className="spinner" /> : 'Sign in'}
            </button>
          </form>

          <p className="auth-switch">
            New institution? <Link to="/register">Register here</Link>
          </p>

          <div className="demo-box">
            <span className="demo-box-title">Demo accounts — click to fill</span>
            <div className="demo-accounts">
              {DEMO_ACCOUNTS.map((account) => (
                <button
                  key={account.email}
                  type="button"
                  className="demo-account"
                  onClick={() => fillDemo(account)}
                >
                  <strong>{account.label}</strong>
                  <span>{account.email}</span>
                  <code>{account.password}</code>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
