import React, { useState, useEffect, useCallback } from 'react';
import { Activity, CreditCard, ShoppingBag, Clock, CheckCircle2, XCircle, LogOut, ShieldCheck, RefreshCw, UserCheck, Calendar } from 'lucide-react';
import { api } from '../api/apiClient';
import { useAuth } from '../context/AuthContext';

export default function EmployeeActivity() {
  const { isAdmin, employee: currentEmployee } = useAuth();
  const [activity, setActivity] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Admin feature: allow inspecting other employees
  const [employeesList, setEmployeesList] = useState([]);
  const [selectedEmpId, setSelectedEmpId] = useState('');

  // Fetch employees list if Admin
  useEffect(() => {
    if (isAdmin) {
      api.get('/api/employees')
        .then(res => res.ok ? res.json() : [])
        .then(data => {
          if (Array.isArray(data)) {
            setEmployeesList(data);
          }
        })
        .catch(() => {});
    }
  }, [isAdmin]);

  const fetchActivity = useCallback(async (targetEmpId) => {
    try {
      setLoading(true);
      setError(null);

      let url = '/api/activity/my';
      if (isAdmin && targetEmpId && targetEmpId.trim()) {
        url = `/api/activity/${targetEmpId.trim()}`;
      }

      const res = await api.get(url);
      if (res.ok) {
        const data = await res.json();
        setActivity(data);
      } else {
        const err = await res.json().catch(() => ({}));
        setError(err.message || 'Failed to retrieve employee activity.');
      }
    } catch (err) {
      setError('Network error loading activity summary.');
    } finally {
      setLoading(false);
    }
  }, [isAdmin]);

  useEffect(() => {
    fetchActivity(selectedEmpId);
  }, [fetchActivity, selectedEmpId]);

  const formatDate = (dateStr) => {
    if (!dateStr) return 'Never / Not recorded';
    try {
      const d = new Date(dateStr);
      return d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
    } catch (e) {
      return dateStr;
    }
  };

  const getEventBadge = (type) => {
    switch (type) {
      case 'LOGIN_SUCCESS':
        return <span className="badge badge-success flex-inline"><CheckCircle2 size={12} /> Login Success</span>;
      case 'LOGIN_FAILURE':
        return <span className="badge badge-danger flex-inline"><XCircle size={12} /> Login Failure</span>;
      case 'LOGOUT':
        return <span className="badge badge-gray flex-inline"><LogOut size={12} /> Logged Out</span>;
      case 'SESSION_EXPIRED':
        return <span className="badge badge-warning flex-inline"><Clock size={12} /> Expired</span>;
      default:
        return <span className="badge badge-gray">{type}</span>;
    }
  };

  return (
    <div className="module-container">
      {/* Module Header */}
      <div className="module-header">
        <div>
          <h2 className="module-title">Employee Performance &amp; Activity</h2>
          <p className="module-subtitle">
            {isAdmin
              ? 'Real-time billing analytics, revenue contribution, and audit logs per employee'
              : `Personal Sales & Session Summary for ${currentEmployee?.fullName || 'Employee'}`}
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          {isAdmin && employeesList.length > 0 && (
            <select
              className="form-control"
              value={selectedEmpId}
              onChange={(e) => setSelectedEmpId(e.target.value)}
              style={{ minWidth: '220px' }}
              id="select-employee-activity"
            >
              <option value="">My Activity ({currentEmployee?.employeeId})</option>
              {employeesList.map(emp => (
                <option key={emp.employeeId} value={emp.employeeId}>
                  {emp.fullName} ({emp.employeeId} - {emp.role})
                </option>
              ))}
            </select>
          )}
          <button
            className="btn btn-secondary"
            onClick={() => fetchActivity(selectedEmpId)}
            disabled={loading}
            id="btn-refresh-activity"
          >
            <RefreshCw size={16} className={loading ? 'spin-animation' : ''} /> Refresh
          </button>
        </div>
      </div>

      {error && (
        <div className="toast toast-error mb-4">
          <span>{error}</span>
        </div>
      )}

      {activity && (
        <>
          {/* Key Metrics Grid */}
          <div className="stats-grid mb-6">
            <div className="stat-card">
              <div className="stat-icon-wrapper bg-emerald-dim">
                <CreditCard size={22} className="text-emerald" />
              </div>
              <div className="stat-content">
                <span className="stat-label">Total Revenue Processed</span>
                <span className="stat-value text-emerald">
                  ₹{Number(activity.totalSalesValue || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
                <span className="stat-subtext">Sum of all customer invoices</span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon-wrapper bg-blue-dim">
                <ShoppingBag size={22} className="text-blue" />
              </div>
              <div className="stat-content">
                <span className="stat-label">Invoices Created</span>
                <span className="stat-value text-blue">
                  {activity.totalBillsCount || 0}
                </span>
                <span className="stat-subtext">Total sales counter transactions</span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon-wrapper bg-purple-dim">
                <UserCheck size={22} className="text-purple" />
              </div>
              <div className="stat-content">
                <span className="stat-label">Employee Profile</span>
                <span className="stat-value-sm font-semibold">
                  {activity.fullName}
                </span>
                <span className="stat-subtext">
                  <span className="code-badge">{activity.employeeId}</span> • Role: <strong className="text-indigo">{activity.role}</strong>
                </span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-icon-wrapper bg-cyan-dim">
                <Clock size={22} className="text-cyan" />
              </div>
              <div className="stat-content">
                <span className="stat-label">Last Login Event</span>
                <span className="stat-value-sm text-cyan font-mono">
                  {formatDate(activity.lastLoginAt)}
                </span>
                <span className="stat-subtext">
                  Status: {activity.active ? <span className="text-emerald">● Active</span> : <span className="text-danger">● Suspended</span>}
                </span>
              </div>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
            {/* Recent Bills Card */}
            <div className="card">
              <div className="card-header">
                <h3 className="card-title">Recent Customer Invoices (Top 5)</h3>
              </div>
              <div className="table-responsive">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Bill ID</th>
                      <th>Date</th>
                      <th>Customer</th>
                      <th style={{ textAlign: 'right' }}>Amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {!activity.recentBills || activity.recentBills.length === 0 ? (
                      <tr>
                        <td colSpan="4" className="text-center py-4 text-muted">
                          No recent bills issued by this employee.
                        </td>
                      </tr>
                    ) : (
                      activity.recentBills.map(b => (
                        <tr key={b.billId || b.id}>
                          <td>
                            <span className="code-badge font-mono">#{(b.billId || b.id).substring(0, 8)}</span>
                          </td>
                          <td className="text-sm font-mono text-cyan">
                            {formatDate(b.billDate)}
                          </td>
                          <td className="font-semibold">
                            {b.customer?.customerName || 'Walk-in'}
                          </td>
                          <td style={{ textAlign: 'right' }} className="font-bold text-emerald">
                            ₹{Number(b.totalAmount || 0).toFixed(2)}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Recent Session Events Card */}
            <div className="card">
              <div className="card-header">
                <h3 className="card-title">Recent Session &amp; Audit Events</h3>
              </div>
              <div className="table-responsive">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Event</th>
                      <th>Status</th>
                      <th>Timestamp</th>
                      <th>IP</th>
                    </tr>
                  </thead>
                  <tbody>
                    {!activity.recentEvents || activity.recentEvents.length === 0 ? (
                      <tr>
                        <td colSpan="4" className="text-center py-4 text-muted">
                          No recent session events recorded in audit database.
                        </td>
                      </tr>
                    ) : (
                      activity.recentEvents.map(evt => (
                        <tr key={evt.id}>
                          <td>{getEventBadge(evt.eventType)}</td>
                          <td>
                            <span className={`badge ${evt.status === 'SUCCESS' ? 'badge-emerald' : 'badge-danger'}`}>
                              {evt.status}
                            </span>
                          </td>
                          <td className="text-xs font-mono text-cyan">
                            {formatDate(evt.timestamp)}
                          </td>
                          <td className="text-xs text-muted">
                            {evt.ipAddress || '—'}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
