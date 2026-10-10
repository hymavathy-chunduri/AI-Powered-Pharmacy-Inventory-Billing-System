import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { api } from '../api/apiClient';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [employee, setEmployee] = useState(null);
  const [loading, setLoading] = useState(true);
  const [authError, setAuthError] = useState(null);

  const checkAuth = useCallback(async () => {
    try {
      setLoading(true);
      const res = await api.get('/api/auth/me');
      if (res.ok) {
        const data = await res.json();
        setEmployee(data);
        setAuthError(null);
      } else {
        setEmployee(null);
      }
    } catch (err) {
      console.warn('Authentication check failed:', err);
      setEmployee(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkAuth();

    const handleUnauthorized = () => {
      setEmployee(null);
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, [checkAuth]);

  const login = async (identifier, password) => {
    setAuthError(null);
    try {
      const res = await api.post('/api/auth/login', { identifier, password });
      const data = await res.json();

      if (res.ok) {
        setEmployee(data);
        setAuthError(null);
        return { success: true, employee: data };
      } else {
        const msg = data.message || 'Authentication failed. Please check your credentials.';
        setAuthError(msg);
        return { success: false, error: msg };
      }
    } catch (err) {
      const msg = 'Unable to connect to authentication server. Please verify backend is running.';
      setAuthError(msg);
      return { success: false, error: msg };
    }
  };

  const logout = async () => {
    try {
      await api.post('/api/auth/logout', {});
    } catch (err) {
      console.warn('Logout request failed:', err);
    } finally {
      setEmployee(null);
      setAuthError(null);
    }
  };

  const isAdmin = employee?.role === 'ADMIN';
  const isPharmacist = employee?.role === 'PHARMACIST';
  const isCashier = employee?.role === 'CASHIER';

  return (
    <AuthContext.Provider
      value={{
        employee,
        loading,
        authError,
        setAuthError,
        login,
        logout,
        checkAuth,
        isAdmin,
        isPharmacist,
        isCashier,
        isAuthenticated: !!employee,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
