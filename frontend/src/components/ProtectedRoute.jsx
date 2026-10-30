import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function FullPageLoader() {
  return (
    <div className="page-loader">
      <span className="spinner spinner-lg" />
      <p>Checking your session…</p>
    </div>
  );
}

export default function ProtectedRoute({ role, children }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <FullPageLoader />;
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (role && user.role !== role) {
    return <Navigate to="/" replace />;
  }

  return children;
}
