import React, { useState, useEffect, useCallback } from 'react';
import { Plus, Edit2, Trash2, Tag, Loader2, AlertCircle, RefreshCw, CheckCircle2, ShieldAlert } from 'lucide-react';
import { api } from '../api/apiClient';
import { useAuth } from '../context/AuthContext';

export default function Categories() {
  const { isAdmin, isPharmacist, isCashier } = useAuth();
  const canManage = isAdmin || isPharmacist;

  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');

  const [showModal, setShowModal] = useState(false);
  const [editCat, setEditCat] = useState(null);
  const [formData, setFormData] = useState({ categoryName: '', description: '' });
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);

  const fetchCategories = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get('/api/categories');
      if (res.ok) {
        const data = await res.json();
        setCategories(Array.isArray(data) ? data : []);
      } else {
        setError(`Failed to load categories (${res.status}). Please check server connection.`);
      }
    } catch (err) {
      setError('Unable to reach backend server. Please verify your connection.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  const handleOpenAdd = () => {
    setEditCat(null);
    setFormData({ categoryName: '', description: '' });
    setShowModal(true);
  };

  const handleOpenEdit = (c) => {
    setEditCat(c);
    setFormData({
      categoryName: c.categoryName || '',
      description: c.description || '',
    });
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!canManage) {
      setToast({ type: 'danger', message: 'Access denied: Only Administrators and Pharmacists can modify categories.' });
      return;
    }

    setSaving(true);
    const catId = editCat ? (editCat.categoryId || editCat.id) : null;
    const url = editCat ? `/api/categories/${catId}` : '/api/categories';
    const method = editCat ? 'put' : 'post';

    try {
      const res = await api[method](url, formData);
      if (res.ok) {
        setToast({
          type: 'success',
          message: `Category "${formData.categoryName}" ${editCat ? 'updated' : 'added'} successfully.`,
        });
        setShowModal(false);
        fetchCategories();
      } else {
        const errData = await res.json().catch(() => ({}));
        setToast({
          type: 'danger',
          message: errData.message || `Failed to save category (${res.status}).`,
        });
      }
    } catch (err) {
      setToast({ type: 'danger', message: 'Network error while saving category.' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id, name) => {
    if (!canManage) {
      setToast({ type: 'danger', message: 'Access denied: Only Administrators and Pharmacists can delete categories.' });
      return;
    }

    if (!window.confirm(`Are you sure you want to delete category "${name}"?`)) return;

    try {
      const res = await api.delete(`/api/categories/${id}`);
      if (res.ok) {
        setToast({ type: 'success', message: `Category "${name}" deleted successfully.` });
        fetchCategories();
      } else {
        setToast({ type: 'danger', message: `Failed to delete category (${res.status}).` });
      }
    } catch (err) {
      setToast({ type: 'danger', message: 'Network error while deleting category.' });
    }
  };

  const filteredCategories = categories.filter((c) => {
    const q = searchTerm.toLowerCase();
    return (
      (c.categoryName && c.categoryName.toLowerCase().includes(q)) ||
      (c.description && c.description.toLowerCase().includes(q))
    );
  });

  return (
    <div>
      <div className="header">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <h1 className="page-title" style={{ margin: 0 }}>Medicine Categories</h1>
            <span className="badge badge-primary">
              {categories.length} Total
            </span>
            {isCashier && (
              <span className="badge badge-secondary" title="Cashier account has read-only access to categories">
                Read-Only
              </span>
            )}
          </div>
          <p className="subtitle">
            Classify pharmaceutical inventory and manage therapeutic classifications
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <button
            className="btn btn-secondary"
            onClick={fetchCategories}
            disabled={loading}
            title="Refresh categories list"
          >
            <RefreshCw size={16} className={loading ? 'spin-animation' : ''} />
            <span>Refresh</span>
          </button>

          {canManage && (
            <button
              className="btn btn-primary"
              onClick={handleOpenAdd}
              id="btn-add-category"
            >
              <Plus size={18} />
              <span>Add Category</span>
            </button>
          )}
        </div>
      </div>

      {toast && (
        <div className={`toast toast-${toast.type}`} style={{ marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            {toast.type === 'success' ? <CheckCircle2 size={16} /> : <AlertCircle size={16} />}
            <span>{toast.message}</span>
          </div>
          <button
            style={{ background: 'none', border: 'none', color: 'inherit', cursor: 'pointer', fontSize: '1.2rem' }}
            onClick={() => setToast(null)}
          >
            ×
          </button>
        </div>
      )}

      {/* Search Bar */}
      <div className="panel" style={{ padding: '0.85rem 1.25rem', marginBottom: '1.25rem' }}>
        <input
          type="text"
          className="form-control"
          placeholder="Search categories by name or therapeutic description..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          style={{ maxWidth: '420px' }}
        />
      </div>

      {/* Main Content Table / States */}
      <div className="panel">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '3.5rem 1rem' }}>
            <Loader2 size={32} className="spin-animation text-cyan" style={{ margin: '0 auto 1rem' }} />
            <p className="text-muted" style={{ margin: 0 }}>Loading medicine categories from database...</p>
          </div>
        ) : error ? (
          <div style={{ textAlign: 'center', padding: '3rem 1rem' }}>
            <AlertCircle size={36} color="#ef4444" style={{ margin: '0 auto 0.75rem' }} />
            <h3 style={{ margin: '0 0 0.5rem', color: '#f87171' }}>Failed to Load Categories</h3>
            <p className="text-muted" style={{ maxWidth: '460px', margin: '0 auto 1.25rem' }}>{error}</p>
            <button className="btn btn-primary btn-sm" onClick={fetchCategories}>
              <RefreshCw size={14} /> Retry Loading
            </button>
          </div>
        ) : filteredCategories.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem 1rem' }}>
            <Tag size={36} color="#64748b" style={{ margin: '0 auto 0.75rem' }} />
            <h3 style={{ margin: '0 0 0.5rem', color: '#cbd5e1' }}>
              {searchTerm ? 'No Matching Categories Found' : 'No Categories Defined Yet'}
            </h3>
            <p className="text-muted" style={{ maxWidth: '420px', margin: '0 auto 1rem' }}>
              {searchTerm
                ? 'Try clearing your search query or search by another keyword.'
                : 'Define therapeutic categories to classify drugs and medicines in the inventory.'}
            </p>
            {searchTerm && (
              <button className="btn btn-secondary btn-sm" onClick={() => setSearchTerm('')}>
                Clear Search
              </button>
            )}
            {!searchTerm && canManage && (
              <button className="btn btn-primary btn-sm" onClick={handleOpenAdd}>
                <Plus size={14} /> Create First Category
              </button>
            )}
          </div>
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th style={{ width: '80px' }}>#</th>
                  <th>Category Name</th>
                  <th>Therapeutic Description</th>
                  {canManage && <th style={{ width: '120px', textAlign: 'right' }}>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {filteredCategories.map((c, idx) => {
                  const id = c.categoryId || c.id;
                  return (
                    <tr key={id || idx}>
                      <td className="text-muted font-mono">{idx + 1}</td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <Tag size={15} color="#38bdf8" />
                          <strong>{c.categoryName}</strong>
                        </div>
                      </td>
                      <td>
                        <span className="text-muted">{c.description || '—'}</span>
                      </td>
                      {canManage && (
                        <td style={{ textAlign: 'right' }}>
                          <div style={{ display: 'inline-flex', gap: '0.4rem' }}>
                            <button
                              className="btn btn-secondary btn-sm"
                              onClick={() => handleOpenEdit(c)}
                              title="Edit Category"
                            >
                              <Edit2 size={13} />
                            </button>
                            <button
                              className="btn btn-danger btn-sm"
                              onClick={() => handleDelete(id, c.categoryName)}
                              title="Delete Category"
                            >
                              <Trash2 size={13} />
                            </button>
                          </div>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal for Add / Edit */}
      {showModal && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '480px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <h2 style={{ margin: 0 }}>{editCat ? 'Edit Category' : 'Add New Category'}</h2>
              <button
                style={{ background: 'none', border: 'none', color: '#94a3b8', fontSize: '1.5rem', cursor: 'pointer' }}
                onClick={() => setShowModal(false)}
              >
                ×
              </button>
            </div>

            <form onSubmit={handleSave}>
              <div className="form-group">
                <label className="form-label">Category Name *</label>
                <input
                  type="text"
                  className="form-control"
                  required
                  placeholder="e.g. Antibiotics, Analgesics, Vitamins"
                  value={formData.categoryName}
                  onChange={(e) => setFormData({ ...formData, categoryName: e.target.value })}
                  disabled={saving}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Therapeutic Description</label>
                <textarea
                  className="form-control"
                  rows="3"
                  placeholder="e.g. Medicines used to treat bacterial infections"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  disabled={saving}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowModal(false)}
                  disabled={saving}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={saving}
                >
                  {saving ? (
                    <>
                      <Loader2 size={16} className="spin-animation" />
                      <span>Saving...</span>
                    </>
                  ) : (
                    <span>{editCat ? 'Save Changes' : 'Create Category'}</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
