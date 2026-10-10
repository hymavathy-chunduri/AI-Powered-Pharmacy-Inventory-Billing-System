import React, { useState, useEffect, useCallback } from 'react';
import { History, Shield, Filter, Search, CheckCircle2, XCircle, LogOut, RefreshCw, Clock, Globe, RotateCcw } from 'lucide-react';
import { api } from '../api/apiClient';
import { useAuth } from '../context/AuthContext';

export default function LoginHistory() {
  const { isAdmin, employee } = useAuth();
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Pagination & filter state
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Input filter states (for the form inputs)
  const [filterEmpId, setFilterEmpId] = useState('');
  const [filterEventType, setFilterEventType] = useState('');
  const [filterStatus, setFilterStatus] = useState('');

  // Applied filter state (triggers the actual fetch)
  const [appliedFilters, setAppliedFilters] = useState({
    empId: '',
    eventType: '',
    status: ''
  });

  const fetchHistory = useCallback(async (targetPage, activeFilters) => {
    try {
      setLoading(true);
      setError(null);

      let url = '';
      if (isAdmin) {
        const params = new URLSearchParams();
        params.append('page', String(targetPage));
        params.append('size', '15');
        if (activeFilters.empId) params.append('employeeId', activeFilters.empId);
        if (activeFilters.eventType) params.append('eventType', activeFilters.eventType);
        if (activeFilters.status) params.append('status', activeFilters.status);
        url = `/api/login-history?${params.toString()}`;
      } else {
        url = `/api/login-history/my?page=${targetPage}&size=15`;
      }

      const res = await api.get(url);
      if (res.ok) {
        const data = await res.json();
        setHistory(data.content || []);
        setTotalPages(data.totalPages || 1);
        setTotalElements(data.totalElements || 0);
      } else {
        const err = await res.json().catch(() => ({}));
        setError(err.message || 'Failed to load login history records');
      }
    } catch (err) {
      setError('Network error while loading audit records.');
    } finally {
      setLoading(false);
    }
  }, [isAdmin]);

  // Single effect: Triggers only when page, applied filters, or admin role change
  useEffect(() => {
    fetchHistory(page, appliedFilters);
  }, [fetchHistory, page, appliedFilters, isAdmin]);

  const handleApplyFilter = (e) => {
    if (e) e.preventDefault();
    setPage(0);
    setAppliedFilters({
      empId: filterEmpId.trim(),
      eventType: filterEventType,
      status: filterStatus
    });
  };

  const handleResetFilter = () => {
    setFilterEmpId('');
    setFilterEventType('');
    setFilterStatus('');
    setPage(0);
    setAppliedFilters({
      empId: '',
      eventType: '',
      status: ''
    });
  };

  const formatTimestamp = (utcString) => {
    if (!utcString) return '—';
    try {
      const date = new Date(utcString);
      return date.toLocaleString(undefined, {
        dateStyle: 'medium',
        timeStyle: 'medium'
      });
    } catch (e) {
      return utcString;
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
      {/* Header */}
      <div className="module-header">
        <div>
          <h2 className="module-title">Login History &amp; Security Audit</h2>
          <p className="module-subtitle">
            {isAdmin
              ? 'Administrator Audit Trail • Immutable session events across all pharmacy personnel'
              : `Personal Activity Log • Recent login and session history for ${employee?.fullName || 'Employee'} (${employee?.employeeId || 'ID'})`}
          </p>
        </div>
        <button
          className="btn btn-secondary"
          onClick={() => fetchHistory(page, appliedFilters)}
          disabled={loading}
          title="Refresh Records"
          id="btn-refresh-history"
        >
          <RefreshCw size={16} className={loading ? 'spin-animation' : ''} /> Refresh
        </button>
      </div>

      {/* Admin Filter Bar */}
      {isAdmin && (
        <form onSubmit={handleApplyFilter} className="filter-bar filter-bar-grid">
          <div className="search-input-wrapper">
            <Search size={16} className="search-icon" />
            <input
              type="text"
              className="form-control"
              placeholder="Filter by Employee ID..."
              value={filterEmpId}
              onChange={(e) => setFilterEmpId(e.target.value)}
              id="filter-employee-id"
            />
          </div>

          <select
            className="form-control"
            value={filterEventType}
            onChange={(e) => setFilterEventType(e.target.value)}
            id="filter-event-type"
          >
            <option value="">All Event Types</option>
            <option value="LOGIN_SUCCESS">LOGIN_SUCCESS</option>
            <option value="LOGIN_FAILURE">LOGIN_FAILURE</option>
            <option value="LOGOUT">LOGOUT</option>
            <option value="SESSION_EXPIRED">SESSION_EXPIRED</option>
          </select>

          <select
            className="form-control"
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            id="filter-status"
          >
            <option value="">All Statuses</option>
            <option value="SUCCESS">SUCCESS</option>
            <option value="FAILURE">FAILURE</option>
          </select>

          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button type="submit" className="btn btn-primary" id="btn-apply-filter">
              <Filter size={16} /> Filter
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleResetFilter}
              title="Reset Filters"
              id="btn-reset-filter"
            >
              <RotateCcw size={16} />
            </button>
          </div>
        </form>
      )}

      {error && (
        <div className="toast toast-error mb-4">
          <span>{error}</span>
        </div>
      )}

      {/* Table */}
      <div className="table-responsive card">
        <table className="table">
          <thead>
            <tr>
              <th>Employee ID</th>
              <th>Employee Name</th>
              <th>Event Type</th>
              <th>Status</th>
              <th>Failure Reason</th>
              <th>Local Timestamp</th>
              <th>IP Address</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="7" className="text-center py-4 text-muted">
                  Loading audit log records from audit database...
                </td>
              </tr>
            ) : history.length === 0 ? (
              <tr>
                <td colSpan="7" className="text-center py-4 text-muted">
                  No login history events found matching the criteria.
                </td>
              </tr>
            ) : (
              history.map((item) => (
                <tr key={item.id}>
                  <td>
                    <span className="code-badge">{item.employeeId}</span>
                  </td>
                  <td className="font-semibold">{item.employeeName || '—'}</td>
                  <td>{getEventBadge(item.eventType)}</td>
                  <td>
                    <span className={`badge ${item.status === 'SUCCESS' ? 'badge-emerald' : 'badge-danger'}`}>
                      {item.status}
                    </span>
                  </td>
                  <td>
                    {item.failureReason ? (
                      <span className="reason-pill">{item.failureReason}</span>
                    ) : (
                      <span className="text-muted">—</span>
                    )}
                  </td>
                  <td className="text-sm font-mono text-cyan">
                    {formatTimestamp(item.timestamp)}
                  </td>
                  <td className="text-sm text-muted">
                    {item.ipAddress ? (
                      <span className="flex-inline"><Globe size={13} /> {item.ipAddress}</span>
                    ) : (
                      '—'
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      <div className="pagination-footer">
        <span className="text-muted text-sm">
          Showing {history.length} of {totalElements} total records (Page {page + 1} of {Math.max(1, totalPages)})
        </span>
        <div className="pagination-buttons">
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0 || loading}
            id="btn-prev-page"
          >
            ← Previous
          </button>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1 || loading}
            id="btn-next-page"
          >
            Next →
          </button>
        </div>
      </div>
    </div>
  );
}
