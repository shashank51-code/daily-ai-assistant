import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext.jsx';

function AuthLoading() {
  return (
    <div className="auth-shell" role="status" aria-live="polite">
      <div className="auth-card">
        <span className="brand-mark" aria-hidden="true">AI</span>
        <p className="muted">Checking your session…</p>
      </div>
    </div>
  );
}

export function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <AuthLoading />;
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return children;
}

export function PublicOnlyRoute({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <AuthLoading />;
  if (user) return <Navigate to="/" replace />;
  return children;
}
