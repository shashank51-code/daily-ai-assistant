import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import * as api from '../api/api.js';
import { clearCsrfToken } from '../api/client.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    api.getMe()
      .then((current) => {
        if (active) setUser(current);
      })
      .catch(() => {
        if (active) setUser(null);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const onUnauthorized = () => setUser(null);
    window.addEventListener('dailyai:unauthorized', onUnauthorized);
    return () => window.removeEventListener('dailyai:unauthorized', onUnauthorized);
  }, []);

  const login = useCallback(async (email, password) => {
    const current = await api.login(email, password);
    setUser(current);
    return current;
  }, []);

  const register = useCallback(async (payload) => {
    const current = await api.register(payload);
    setUser(current);
    return current;
  }, []);

  const signOut = useCallback(async () => {
    try {
      await api.logout();
    } catch {
      // An expired/invalid session has nothing left to invalidate.
    } finally {
      clearCsrfToken();
      setUser(null);
    }
  }, []);

  const value = useMemo(
    () => ({ user, loading, login, register, signOut }),
    [user, loading, login, register, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
