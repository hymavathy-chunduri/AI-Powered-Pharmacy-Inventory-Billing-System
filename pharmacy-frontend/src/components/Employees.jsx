import React, { useState, useEffect } from 'react';
import { Users, UserPlus, Shield, KeyRound, CheckCircle2, XCircle, Search, Edit2, AlertCircle, RefreshCw, Lock } from 'lucide-react';
import { api } from '../api/apiClient';

export default function Employees() {
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Modal states
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showResetModal, setShowResetModal] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState(null);

  // Form states
  const [formData, setFormData] = useState({
    employeeId: '',
    fullName: '',
    email: '',
    password: '',
    role: 'PHARMACIST'
  });
  const [editFormData, setEditFormData] = useState({
    fullName: '',
    role: 'PHARMACIST',
    active: true
  });
  const [newPassword, setNewPassword] = useState('');
  const [formError, setFormError] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchEmployees = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await api.get('/api/employees');
      if (res.ok) {
        const data = await res.json();
        setEmployees(data);
      } else {
        const err = await res.json();
        setError(err.message || 'Failed to fetch employees');
      }
    } catch (err) {
      setError('Network error while fetching employees.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEmployees();
  }, []);

  const handleCreate = async (e) => {
    e.preventDefault();
    setFormError(null);
    setActionLoading(true);

    try {
      const res = await api.post('/api/employees', formData);
      const data = await res.json();

      if (res.ok) {
        setSuccessMsg(`Employee account ${data.employeeId} created successfully.`);
        setShowAddModal(false);
        setFormData({ employeeId: '', fullName: '', email: '', password: '', role: 'PHARMACIST' });
        fetchEmployees();
      } else {
        setFormError(data.message || 'Failed to create employee');
      }
    } catch (err) {
      setFormError('Failed to communicate with server.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleEdit = async (e) => {
    e.preventDefault();
    if (!selectedEmployee) return;
    setFormError(null);
    setActionLoading(true);

    try {
      const res = await api.put(`/api/employees/${selectedEmployee.id}`, editFormData);
      const data = await res.json();

      if (res.ok) {
        setSuccessMsg(`Employee ${data.employeeId} updated successfully.`);
        setShowEditModal(false);
        setSelectedEmployee(null);
        fetchEmployees();
      } else {
        setFormError(data.message || 'Failed to update employee');
      }
    } catch (err) {
      setFormError('Failed to communicate with server.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    if (!selectedEmployee) return;
    setFormError(null);
    setActionLoading(true);

    try {
      const res = await api.put(`/api/employees/${selectedEmployee.id}/reset-password`, { newPassword });
      const data = await res.json();

      if (res.ok) {
        setSuccessMsg(`Password for ${selectedEmployee.employeeId} has been reset.`);
        setShowResetModal(false);
        setSelectedEmployee(null);
        setNewPassword('');
      } else {
        setFormError(data.message || 'Failed to reset password');
      }
    } catch (err) {
      setFormError('Failed to communicate with server.');
    } finally {
      setActionLoading(false);
    }
  };

  const toggleStatus = async (emp) => {
    try {
      const res = await api.put(`/api/employees/${emp.id}/status`, { active: !emp.active });
      if (res.ok) {
        setSuccessMsg(`Employee ${emp.employeeId} status set to ${!emp.active ? 'ACTIVE' : 'INACTIVE'}.`);
        fetchEmployees();
      } else {
        const err = await res.json();
        setError(err.message || 'Status update failed.');
      }
    } catch (err) {
      setError('Network error while updating status.');
    }
  };

  const filteredEmployees = employees.filter(e => {
    const q = searchQuery.toLowerCase();
    return (
      e.employeeId?.toLowerCase().includes(q) ||
      e.fullName?.toLowerCase().includes(q) ||
      e.email?.toLowerCase().includes(q) ||
      e.role?.toLowerCase().includes(q)
    );
  });

  const getRoleBadgeClass = (role) => {
    switch (role) {
      case 'ADMIN': return 'badge-purple';
      case 'PHARMACIST': return 'badge-emerald';
      case 'CASHIER': return 'badge-cyan';
      default: return 'badge-gray';
    }
  };

  return (
    <div className="module-container">
      {/* Module Header */}
      <div className="module-header">
        <div>
          <h2 className="module-title">Employee Management</h2>
          <p className="module-subtitle">Administrator Access • Manage pharmacy staff accounts, roles &amp; security</p>
        </div>
        <button
          className="btn btn-primary"
          onClick={() => {
            setFormError(null);
            setShowAddModal(true);
          }}
          id="btn-add-employee"
        >
          <UserPlus size={18} /> Provision New Employee
        </button>
      </div>

      {/* Notifications */}
      {successMsg && (
        <div className="toast toast-success">
          <span>{successMsg}</span>
          <button className="btn-close" onClick={() => setSuccessMsg(null)}>×</button>
        </div>
      )}
      {error && (
        <div className="toast toast-error">
          <span>{error}</span>
          <button className="btn-close" onClick={() => setError(null)}>×</button>
        </div>
      )}

      {/* Search Bar */}
      <div className="filter-bar">
        <div className="search-input-wrapper">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            className="form-control"
            placeholder="Search by Employee ID, Name, Email or Role..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        <button className="btn btn-secondary" onClick={fetchEmployees} title="Refresh">
          <RefreshCw size={16} /> Refresh
        </button>
      </div>

      {/* Employee Table */}
      <div className="table-responsive card">
        <table className="table">
          <thead>
            <tr>
              <th>Employee ID</th>
              <th>Full Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Last Login</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="7" className="text-center py-4">Loading employee records...</td>
              </tr>
            ) : filteredEmployees.length === 0 ? (
              <tr>
                <td colSpan="7" className="text-center py-4">No employee accounts found.</td>
              </tr>
            ) : (
              filteredEmployees.map((emp) => (
                <tr key={emp.id}>
                  <td>
                    <span className="code-badge">{emp.employeeId}</span>
                  </td>
                  <td className="font-semibold">{emp.fullName}</td>
                  <td className="text-muted">{emp.email}</td>
                  <td>
                    <span className={`badge ${getRoleBadgeClass(emp.role)}`}>
                      {emp.role}
                    </span>
                  </td>
                  <td>
                    {emp.active ? (
                      <span className="badge badge-success flex-inline">
                        <CheckCircle2 size={12} /> Active
                      </span>
                    ) : (
                      <span className="badge badge-danger flex-inline">
                        <XCircle size={12} /> Inactive
                      </span>
                    )}
                  </td>
                  <td className="text-muted text-sm">
                    {emp.lastLoginAt ? new Date(emp.lastLoginAt).toLocaleString() : 'Never'}
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="btn-action btn-action-edit"
                        title="Edit Details"
                        onClick={() => {
                          setSelectedEmployee(emp);
                          setEditFormData({ fullName: emp.fullName, role: emp.role, active: emp.active });
                          setFormError(null);
                          setShowEditModal(true);
                        }}
                      >
                        <Edit2 size={16} />
                      </button>
                      <button
                        className="btn-action btn-action-key"
                        title="Reset Password"
                        onClick={() => {
                          setSelectedEmployee(emp);
                          setNewPassword('');
                          setFormError(null);
                          setShowResetModal(true);
                        }}
                      >
                        <KeyRound size={16} />
                      </button>
                      <button
                        className={`btn-action ${emp.active ? 'btn-action-deactivate' : 'btn-action-activate'}`}
                        title={emp.active ? 'Deactivate Account' : 'Activate Account'}
                        onClick={() => toggleStatus(emp)}
                      >
                        {emp.active ? <XCircle size={16} /> : <CheckCircle2 size={16} />}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Add Employee Modal */}
      {showAddModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 className="modal-title">Provision New Employee Account</h3>
            <p className="modal-subtitle">Authorized pharmacy staff only. Passwords will be securely hashed with BCrypt.</p>

            {formError && (
              <div className="toast toast-error mb-4">
                <AlertCircle size={16} /> <span>{formError}</span>
              </div>
            )}

            <form onSubmit={handleCreate}>
              <div className="form-group">
                <label className="form-label">Employee ID *</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="e.g. EMP-002"
                  value={formData.employeeId}
                  onChange={(e) => setFormData({ ...formData, employeeId: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Full Name *</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="e.g. John Doe"
                  value={formData.fullName}
                  onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Email Address *</label>
                <input
                  type="email"
                  className="form-control"
                  placeholder="e.g. john@pharmacare.com"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Initial Password * (min 6 characters)</label>
                <input
                  type="password"
                  className="form-control"
                  placeholder="••••••••••••"
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  minLength="6"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Role *</label>
                <select
                  className="form-control"
                  value={formData.role}
                  onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                >
                  <option value="PHARMACIST">PHARMACIST (Inventory, Dispensing, Purchases, Reports)</option>
                  <option value="CASHIER">CASHIER (POS Billing Counter, Customers)</option>
                  <option value="ADMIN">ADMIN (Full System Administration &amp; Staff Management)</option>
                </select>
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowAddModal(false)}
                  disabled={actionLoading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={actionLoading}
                >
                  {actionLoading ? 'Creating Account...' : 'Create Employee'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Employee Modal */}
      {showEditModal && selectedEmployee && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 className="modal-title">Edit Employee: {selectedEmployee.employeeId}</h3>
            <p className="modal-subtitle">Update details and role permissions</p>

            {formError && (
              <div className="toast toast-error mb-4">
                <AlertCircle size={16} /> <span>{formError}</span>
              </div>
            )}

            <form onSubmit={handleEdit}>
              <div className="form-group">
                <label className="form-label">Full Name</label>
                <input
                  type="text"
                  className="form-control"
                  value={editFormData.fullName}
                  onChange={(e) => setEditFormData({ ...editFormData, fullName: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Role</label>
                <select
                  className="form-control"
                  value={editFormData.role}
                  onChange={(e) => setEditFormData({ ...editFormData, role: e.target.value })}
                >
                  <option value="PHARMACIST">PHARMACIST</option>
                  <option value="CASHIER">CASHIER</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Account Status</label>
                <select
                  className="form-control"
                  value={editFormData.active ? 'true' : 'false'}
                  onChange={(e) => setEditFormData({ ...editFormData, active: e.target.value === 'true' })}
                >
                  <option value="true">Active (Permitted to log in)</option>
                  <option value="false">Inactive (Login blocked)</option>
                </select>
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => { setShowEditModal(false); setSelectedEmployee(null); }}
                  disabled={actionLoading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={actionLoading}
                >
                  {actionLoading ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reset Password Modal */}
      {showResetModal && selectedEmployee && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3 className="modal-title">Reset Password: {selectedEmployee.employeeId}</h3>
            <p className="modal-subtitle">Employee: {selectedEmployee.fullName} ({selectedEmployee.email})</p>

            {formError && (
              <div className="toast toast-error mb-4">
                <AlertCircle size={16} /> <span>{formError}</span>
              </div>
            )}

            <form onSubmit={handleResetPassword}>
              <div className="form-group">
                <label className="form-label">New Password (min 6 characters)</label>
                <input
                  type="password"
                  className="form-control"
                  placeholder="••••••••••••"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  minLength="6"
                  required
                  autoFocus
                />
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => { setShowResetModal(false); setSelectedEmployee(null); }}
                  disabled={actionLoading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={actionLoading}
                >
                  {actionLoading ? 'Resetting...' : 'Confirm Reset Password'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
