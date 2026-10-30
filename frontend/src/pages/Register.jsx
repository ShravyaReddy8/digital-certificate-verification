import { useState } from 'react';
import { Link } from 'react-router-dom';
import { getErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

const EMPTY_FORM = {
  name: '',
  institutionName: '',
  email: '',
  password: '',
  confirmPassword: '',
};

export default function Register() {
  const { register } = useAuth();
  const { addToast } = useToast();

  const [form, setForm] = useState(EMPTY_FORM);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(null);

  const handleChange = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
    setErrors((current) => ({ ...current, [event.target.name]: undefined }));
    setError('');
  };

  const validate = () => {
    const next = {};
    if (form.name.trim().length < 2) next.name = 'Enter your full name.';
    if (form.institutionName.trim().length < 2) next.institutionName = 'Enter your institution name.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) next.email = 'Enter a valid email address.';
    if (form.password.length < 8) next.password = 'Password must be at least 8 characters.';
    else if (!/[A-Za-z]/.test(form.password) || !/\d/.test(form.password)) {
      next.password = 'Password must contain at least one letter and one number.';
    }
    if (form.confirmPassword !== form.password) next.confirmPassword = 'Passwords do not match.';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!validate()) return;
    setSubmitting(true);
    setError('');
    try {
      const response = await register({
        name: form.name.trim(),
        institutionName: form.institutionName.trim(),
        email: form.email.trim(),
        password: form.password,
      });
      setSuccess(response);
      setForm(EMPTY_FORM);
      addToast('Registration submitted for administrator approval.', 'success');
    } catch (registerError) {
      setError(getErrorMessage(registerError));
    } finally {
      setSubmitting(false);
    }
  };

  if (success) {
    return (
      <div className="page auth-page">
        <div className="container page-narrow">
          <div className="card success-card">
            <div className="success-icon">✓</div>
            <h1>Registration received</h1>
            <p>{success.message}</p>
            <div className="success-summary">
              <div>
                <span>Institution</span>
                <strong>{success.user.institutionName}</strong>
              </div>
              <div>
                <span>Account email</span>
                <strong>{success.user.email}</strong>
              </div>
              <div>
                <span>Status</span>
                <strong>Pending approval</strong>
              </div>
            </div>
            <p className="muted">
              An administrator must approve your institution before you can sign in and issue
              certificates. You will be able to log in as soon as approval is granted.
            </p>
            <Link to="/login" className="btn btn-primary">
              Go to sign in
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="page auth-page">
      <div className="container auth-grid">
        <div className="auth-aside">
          <p className="eyebrow">For institutions</p>
          <h1>Become a trusted certificate issuer</h1>
          <p>
            Register your institution to issue cryptographically signed certificates with QR codes,
            manage revocation and let employers verify credentials instantly.
          </p>
          <ul className="check-list">
            <li>Auto-generated professional PDF certificates</li>
            <li>Upload existing PDF or image certificates</li>
            <li>Unique certificate IDs and QR codes</li>
            <li>Revocation with a documented reason</li>
          </ul>
          <div className="auth-art">
            <span className="auth-art-shield">🛡</span>
            <div>
              <strong>Approval keeps the registry trustworthy</strong>
              <small>Every issuer is reviewed by an administrator before issuing.</small>
            </div>
          </div>
        </div>

        <div className="card auth-card">
          <h2>Register your institution</h2>
          <p className="card-subtitle">All fields are required. Approval is usually quick.</p>

          {error && <div className="alert alert-danger">{error}</div>}

          <form onSubmit={handleSubmit} className="stack-form" noValidate>
            <label className="field">
              <span className="field-label">Full name</span>
              <input
                type="text"
                name="name"
                value={form.name}
                onChange={handleChange}
                placeholder="Registrar or authorised officer"
                autoComplete="name"
                className={errors.name ? 'invalid' : ''}
              />
              {errors.name && <span className="field-error">{errors.name}</span>}
            </label>

            <label className="field">
              <span className="field-label">Institution name</span>
              <input
                type="text"
                name="institutionName"
                value={form.institutionName}
                onChange={handleChange}
                placeholder="e.g. Springfield Institute of Technology"
                autoComplete="organization"
                className={errors.institutionName ? 'invalid' : ''}
              />
              {errors.institutionName && (
                <span className="field-error">{errors.institutionName}</span>
              )}
            </label>

            <label className="field">
              <span className="field-label">Official email</span>
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                placeholder="registrar@institution.edu"
                autoComplete="email"
                className={errors.email ? 'invalid' : ''}
              />
              {errors.email && <span className="field-error">{errors.email}</span>}
            </label>

            <div className="field-row">
              <label className="field">
                <span className="field-label">Password</span>
                <input
                  type="password"
                  name="password"
                  value={form.password}
                  onChange={handleChange}
                  placeholder="At least 8 characters"
                  autoComplete="new-password"
                  className={errors.password ? 'invalid' : ''}
                />
                {errors.password && <span className="field-error">{errors.password}</span>}
              </label>

              <label className="field">
                <span className="field-label">Confirm password</span>
                <input
                  type="password"
                  name="confirmPassword"
                  value={form.confirmPassword}
                  onChange={handleChange}
                  placeholder="Repeat password"
                  autoComplete="new-password"
                  className={errors.confirmPassword ? 'invalid' : ''}
                />
                {errors.confirmPassword && (
                  <span className="field-error">{errors.confirmPassword}</span>
                )}
              </label>
            </div>

            <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
              {submitting ? <span className="spinner" /> : 'Create issuer account'}
            </button>
          </form>

          <p className="auth-switch">
            Already registered? <Link to="/login">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
