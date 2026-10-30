import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function LogoMark() {
  return (
    <svg viewBox="0 0 40 40" width="34" height="34" aria-hidden="true" className="logo-mark">
      <rect width="40" height="40" rx="10" fill="#2563eb" />
      <path d="M20 7l9 3.2v7.6c0 6.4-3.8 10.9-9 12.9-5.2-2-9-6.5-9-12.9v-7.6L20 7z" fill="#ffffff" />
      <path d="M15 19.4l3.6 3.6 6.4-7" stroke="#2563eb" strokeWidth="2.6" fill="none" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const closeMenu = () => setOpen(false);

  const handleLogout = () => {
    logout();
    closeMenu();
    navigate('/');
  };

  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <Link to="/" className="brand" onClick={closeMenu}>
          <LogoMark />
          <span className="brand-text">
            Certi<span>Trust</span>
          </span>
        </Link>

        <button
          type="button"
          className="nav-toggle"
          aria-label="Toggle navigation menu"
          aria-expanded={open}
          onClick={() => setOpen((value) => !value)}
        >
          <span />
          <span />
          <span />
        </button>

        <nav className={`nav-links ${open ? 'open' : ''}`}>
          <NavLink to="/" onClick={closeMenu} end>
            Home
          </NavLink>
          <NavLink to="/verify" onClick={closeMenu}>
            Verify
          </NavLink>
          {user?.role === 'ISSUER' && (
            <NavLink to="/issuer" onClick={closeMenu}>
              Issuer Dashboard
            </NavLink>
          )}
          {user?.role === 'ADMIN' && (
            <NavLink to="/admin" onClick={closeMenu}>
              Admin Dashboard
            </NavLink>
          )}

          <div className="nav-actions">
            {user ? (
              <>
                <span className="user-chip" title={user.email}>
                  <span className="user-avatar">{user.name?.charAt(0)?.toUpperCase() || 'U'}</span>
                  <span className="user-meta">
                    <strong>{user.name}</strong>
                    <small>{user.role === 'ADMIN' ? 'Administrator' : 'Issuer'}</small>
                  </span>
                </span>
                <button type="button" className="btn btn-ghost btn-sm" onClick={handleLogout}>
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="btn btn-ghost btn-sm" onClick={closeMenu}>
                  Sign in
                </Link>
                <Link to="/register" className="btn btn-primary btn-sm" onClick={closeMenu}>
                  Register institution
                </Link>
              </>
            )}
          </div>
        </nav>
      </div>
    </header>
  );
}
