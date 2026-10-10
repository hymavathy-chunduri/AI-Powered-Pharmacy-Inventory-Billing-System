import React, { useState } from 'react';
import { Pill, Lock, User, Eye, EyeOff, AlertCircle, Loader2, ShieldCheck, Activity, UserPlus, CheckCircle2, Phone, Mail, Hash, Key } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/apiClient';

export default function Login({ onSuccess }) {
  const { login } = useAuth();
  const [activeMode, setActiveMode] = useState('login'); // 'login' or 'register'

  // Login form state
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loginLoading, setLoginLoading] = useState(false);
  const [loginError, setLoginError] = useState('');
  const [loginFieldErrors, setLoginFieldErrors] = useState({});

  // Registration form state
  const [regData, setRegData] = useState({
    firstName: '',
    lastName: '',
    employeeId: '',
    employeeCode: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: ''
  });
  const [showRegPassword, setShowRegPassword] = useState(false);
  const [showRegConfirmPassword, setShowRegConfirmPassword] = useState(false);
  const [regLoading, setRegLoading] = useState(false);
  const [regError, setRegError] = useState('');
  const [regSuccess, setRegSuccess] = useState('');
  const [regFieldErrors, setRegFieldErrors] = useState({});

  // Login validation
  const validateLogin = () => {
    const errors = {};
    if (!identifier.trim()) {
      errors.identifier = 'Employee ID or registered email is required.';
    }
    if (!password) {
      errors.password = 'Password is required.';
    }
    setLoginFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setLoginError('');

    if (loginLoading) return;
    if (!validateLogin()) return;

    setLoginLoading(true);
    try {
      const result = await login(identifier.trim(), password);
      if (result.success) {
        if (onSuccess) onSuccess();
      } else {
        setLoginError(result.error || 'Authentication failed. Please check your credentials.');
      }
    } catch (err) {
      setLoginError('A network error occurred. Please try again.');
    } finally {
      setLoginLoading(false);
    }
  };

  // Registration validation — server validates actual authorization code
  const validateRegistration = () => {
    const errors = {};
    if (!regData.firstName.trim()) errors.firstName = 'First name is required.';
    if (!regData.lastName.trim()) errors.lastName = 'Last name is required.';
    if (!regData.employeeId.trim()) errors.employeeId = 'Employee ID is required.';
    if (!regData.employeeCode.trim()) {
      errors.employeeCode = 'Employee registration code is required.';
    }
    if (!regData.email.trim()) {
      errors.email = 'Email address is required.';
    } else if (!/\S+@\S+\.\S+/.test(regData.email.trim())) {
      errors.email = 'Invalid email address format.';
    }
    if (!regData.phone.trim()) errors.phone = 'Phone number is required.';
    if (!regData.password) {
      errors.password = 'Password is required.';
    } else if (regData.password.length < 6) {
      errors.password = 'Password must be at least 6 characters.';
    }
    if (!regData.confirmPassword) {
      errors.confirmPassword = 'Confirm password is required.';
    } else if (regData.password !== regData.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match.';
    }

    setRegFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    setRegError('');
    setRegSuccess('');

    if (regLoading) return;
    if (!validateRegistration()) return;

    setRegLoading(true);
    try {
      const res = await api.post('/api/auth/register', regData);
      const data = await res.json();

      if (res.ok) {
        setRegSuccess(`Account successfully created for ${data.fullName || data.employeeId}! You can now sign in.`);
        setRegData({
          firstName: '',
          lastName: '',
          employeeId: '',
          employeeCode: '',
          email: '',
          phone: '',
          password: '',
          confirmPassword: ''
        });
      } else {
        setRegError(data.message || 'Registration failed. Please check your details.');
      }
    } catch (err) {
      setRegError('Unable to connect to registration server. Please try again.');
    } finally {
      setRegLoading(false);
    }
  };

  return (
    <div className="login-container">
      {/* Background medical ambient elements */}
      <div className="login-backdrop">
        <div className="ambient-glow glow-1"></div>
        <div className="ambient-glow glow-2"></div>
      </div>

      <div className={`login-card ${activeMode === 'register' ? 'login-card-wide' : ''}`}>
        {/* Medical Cross / Brand Header */}
        <div className="login-header">
          <div className="login-logo-badge">
            <Pill size={36} className="brand-icon-svg" />
            <div className="medical-cross-accent">+</div>
          </div>
          <h1 className="login-title">PharmaCare</h1>
          <p className="login-app-name">Pharmacy Inventory &amp; Billing System</p>
          <div className="login-role-tag">
            <ShieldCheck size={14} />
            <span>Authorized Pharmacy Staff Portal</span>
          </div>
        </div>

        {/* Tab Toggle: Login vs Register */}
        <div className="auth-tab-switch">
          <button
            type="button"
            className={`auth-tab-btn ${activeMode === 'login' ? 'active' : ''}`}
            onClick={() => {
              setActiveMode('login');
              setLoginError('');
              setRegError('');
              setRegSuccess('');
            }}
            id="tab-employee-login"
          >
            <Lock size={15} /> Employee Login
          </button>
          <button
            type="button"
            className={`auth-tab-btn ${activeMode === 'register' ? 'active' : ''}`}
            onClick={() => {
              setActiveMode('register');
              setLoginError('');
              setRegError('');
              setRegSuccess('');
            }}
            id="tab-new-employee-register"
          >
            <UserPlus size={15} /> New Employee Registration
          </button>
        </div>

        {/* ===================== MODE 1: LOGIN ===================== */}
        {activeMode === 'login' && (
          <>
            <div className="login-notice-banner">
              <Activity size={14} className="notice-icon" />
              <span>Restricted system. Access is strictly limited to authorized pharmacy staff.</span>
            </div>

            {loginError && (
              <div className="login-error-alert" role="alert">
                <AlertCircle size={18} className="error-alert-icon" />
                <div className="error-alert-text">{loginError}</div>
              </div>
            )}

            <form onSubmit={handleLoginSubmit} className="login-form" noValidate>
              {/* Identifier */}
              <div className="form-group">
                <label htmlFor="employee-identifier" className="form-label">
                  Employee ID or Registered Email
                </label>
                <div className="input-wrapper">
                  <User size={18} className="input-prefix-icon" />
                  <input
                    id="employee-identifier"
                    type="text"
                    className={`form-control input-with-icon ${loginFieldErrors.identifier ? 'input-error' : ''}`}
                    placeholder="e.g. EMP-001 or name@pharmacare.com"
                    value={identifier}
                    onChange={(e) => {
                      setIdentifier(e.target.value);
                      if (loginFieldErrors.identifier) setLoginFieldErrors(prev => ({ ...prev, identifier: null }));
                    }}
                    disabled={loginLoading}
                    autoFocus
                    autoComplete="username"
                  />
                </div>
                {loginFieldErrors.identifier && (
                  <span className="field-error-text">{loginFieldErrors.identifier}</span>
                )}
              </div>

              {/* Password */}
              <div className="form-group">
                <label htmlFor="employee-password" className="form-label">
                  Password
                </label>
                <div className="input-wrapper">
                  <Lock size={18} className="input-prefix-icon" />
                  <input
                    id="employee-password"
                    type={showPassword ? 'text' : 'password'}
                    className={`form-control input-with-icon input-with-suffix ${loginFieldErrors.password ? 'input-error' : ''}`}
                    placeholder="••••••••••••"
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      if (loginFieldErrors.password) setLoginFieldErrors(prev => ({ ...prev, password: null }));
                    }}
                    disabled={loginLoading}
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    className="password-toggle-btn"
                    onClick={() => setShowPassword(!showPassword)}
                    tabIndex={-1}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
                {loginFieldErrors.password && (
                  <span className="field-error-text">{loginFieldErrors.password}</span>
                )}
              </div>

              <button
                type="submit"
                className="btn btn-primary login-submit-btn"
                disabled={loginLoading}
                id="employee-login-submit-btn"
              >
                {loginLoading ? (
                  <>
                    <Loader2 size={18} className="spin-animation" />
                    <span>Verifying Employee Credentials...</span>
                  </>
                ) : (
                  <>
                    <Lock size={18} />
                    <span>Log In to Pharmacy System</span>
                  </>
                )}
              </button>
            </form>
          </>
        )}

        {/* ===================== MODE 2: SELF-REGISTRATION ===================== */}
        {activeMode === 'register' && (
          <>
            <div className="login-notice-banner">
              <Key size={14} className="notice-icon" />
              <span>Self-registration requires a designated authorization code provided by pharmacy admin.</span>
            </div>

            {regSuccess && (
              <div className="login-success-alert" role="alert">
                <CheckCircle2 size={18} className="success-alert-icon" />
                <div className="alert-content">
                  <div>{regSuccess}</div>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-success mt-2"
                    onClick={() => setActiveMode('login')}
                  >
                    Proceed to Employee Login →
                  </button>
                </div>
              </div>
            )}

            {regError && (
              <div className="login-error-alert" role="alert">
                <AlertCircle size={18} className="error-alert-icon" />
                <div className="error-alert-text">{regError}</div>
              </div>
            )}

            <form onSubmit={handleRegisterSubmit} className="register-form" noValidate>
              <div className="form-row-2">
                {/* First Name */}
                <div className="form-group">
                  <label className="form-label">First Name *</label>
                  <input
                    type="text"
                    className={`form-control ${regFieldErrors.firstName ? 'input-error' : ''}`}
                    placeholder="e.g. Sarah"
                    value={regData.firstName}
                    onChange={(e) => setRegData({ ...regData, firstName: e.target.value })}
                    disabled={regLoading}
                    required
                  />
                  {regFieldErrors.firstName && <span className="field-error-text">{regFieldErrors.firstName}</span>}
                </div>

                {/* Last Name */}
                <div className="form-group">
                  <label className="form-label">Last Name *</label>
                  <input
                    type="text"
                    className={`form-control ${regFieldErrors.lastName ? 'input-error' : ''}`}
                    placeholder="e.g. Jenkins"
                    value={regData.lastName}
                    onChange={(e) => setRegData({ ...regData, lastName: e.target.value })}
                    disabled={regLoading}
                    required
                  />
                  {regFieldErrors.lastName && <span className="field-error-text">{regFieldErrors.lastName}</span>}
                </div>
              </div>

              <div className="form-row-2">
                {/* Employee ID */}
                <div className="form-group">
                  <label className="form-label">Employee ID *</label>
                  <div className="input-wrapper">
                    <Hash size={16} className="input-prefix-icon" />
                    <input
                      type="text"
                      className={`form-control input-with-icon ${regFieldErrors.employeeId ? 'input-error' : ''}`}
                      placeholder="e.g. EMP-102"
                      value={regData.employeeId}
                      onChange={(e) => setRegData({ ...regData, employeeId: e.target.value })}
                      disabled={regLoading}
                      required
                    />
                  </div>
                  {regFieldErrors.employeeId && <span className="field-error-text">{regFieldErrors.employeeId}</span>}
                </div>

                {/* Employee Code */}
                <div className="form-group">
                  <label className="form-label">Registration Code *</label>
                  <div className="input-wrapper">
                    <Key size={16} className="input-prefix-icon" />
                    <input
                      type="password"
                      className={`form-control input-with-icon ${regFieldErrors.employeeCode ? 'input-error' : ''}`}
                      placeholder="Enter authorization code"
                      value={regData.employeeCode}
                      onChange={(e) => setRegData({ ...regData, employeeCode: e.target.value })}
                      disabled={regLoading}
                      required
                    />
                  </div>
                  {regFieldErrors.employeeCode && <span className="field-error-text">{regFieldErrors.employeeCode}</span>}
                </div>
              </div>

              <div className="form-row-2">
                {/* Email */}
                <div className="form-group">
                  <label className="form-label">Email Address *</label>
                  <div className="input-wrapper">
                    <Mail size={16} className="input-prefix-icon" />
                    <input
                      type="email"
                      className={`form-control input-with-icon ${regFieldErrors.email ? 'input-error' : ''}`}
                      placeholder="name@pharmacare.com"
                      value={regData.email}
                      onChange={(e) => setRegData({ ...regData, email: e.target.value })}
                      disabled={regLoading}
                      required
                    />
                  </div>
                  {regFieldErrors.email && <span className="field-error-text">{regFieldErrors.email}</span>}
                </div>

                {/* Phone */}
                <div className="form-group">
                  <label className="form-label">Phone Number *</label>
                  <div className="input-wrapper">
                    <Phone size={16} className="input-prefix-icon" />
                    <input
                      type="tel"
                      className={`form-control input-with-icon ${regFieldErrors.phone ? 'input-error' : ''}`}
                      placeholder="e.g. +1 555-0199"
                      value={regData.phone}
                      onChange={(e) => setRegData({ ...regData, phone: e.target.value })}
                      disabled={regLoading}
                      required
                    />
                  </div>
                  {regFieldErrors.phone && <span className="field-error-text">{regFieldErrors.phone}</span>}
                </div>
              </div>

              <div className="form-row-2">
                {/* Password */}
                <div className="form-group">
                  <label className="form-label">Password * (min 6)</label>
                  <div className="input-wrapper">
                    <Lock size={16} className="input-prefix-icon" />
                    <input
                      type={showRegPassword ? 'text' : 'password'}
                      className={`form-control input-with-icon input-with-suffix ${regFieldErrors.password ? 'input-error' : ''}`}
                      placeholder="••••••••"
                      value={regData.password}
                      onChange={(e) => setRegData({ ...regData, password: e.target.value })}
                      disabled={regLoading}
                      minLength={6}
                      required
                    />
                    <button
                      type="button"
                      className="password-toggle-btn"
                      onClick={() => setShowRegPassword(!showRegPassword)}
                      tabIndex={-1}
                    >
                      {showRegPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                  {regFieldErrors.password && <span className="field-error-text">{regFieldErrors.password}</span>}
                </div>

                {/* Confirm Password */}
                <div className="form-group">
                  <label className="form-label">Confirm Password *</label>
                  <div className="input-wrapper">
                    <Lock size={16} className="input-prefix-icon" />
                    <input
                      type={showRegConfirmPassword ? 'text' : 'password'}
                      className={`form-control input-with-icon input-with-suffix ${regFieldErrors.confirmPassword ? 'input-error' : ''}`}
                      placeholder="••••••••"
                      value={regData.confirmPassword}
                      onChange={(e) => setRegData({ ...regData, confirmPassword: e.target.value })}
                      disabled={regLoading}
                      required
                    />
                    <button
                      type="button"
                      className="password-toggle-btn"
                      onClick={() => setShowRegConfirmPassword(!showRegConfirmPassword)}
                      tabIndex={-1}
                    >
                      {showRegConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                  {regFieldErrors.confirmPassword && <span className="field-error-text">{regFieldErrors.confirmPassword}</span>}
                </div>
              </div>

              <button
                type="submit"
                className="btn btn-primary login-submit-btn mt-2"
                disabled={regLoading}
                id="employee-register-submit-btn"
              >
                {regLoading ? (
                  <>
                    <Loader2 size={18} className="spin-animation" />
                    <span>Creating Employee Account...</span>
                  </>
                ) : (
                  <>
                    <UserPlus size={18} />
                    <span>Register New Employee Account</span>
                  </>
                )}
              </button>
            </form>
          </>
        )}

        {/* Footer Notice */}
        <div className="login-footer">
          <p className="footer-policy">
            {activeMode === 'login'
              ? 'Authorized staff only. Session activity and login timestamps are audited.'
              : 'Self-registered accounts receive Cashier permissions. Administrators can promote roles.'}
          </p>
          <div className="footer-version">
            <span>PharmaCare v1.0.0</span> • <span>MongoDB Atlas &amp; Audit DB Secured</span>
          </div>
        </div>
      </div>
    </div>
  );
}
