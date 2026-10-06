import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2 } from 'lucide-react';

export default function Categories() {
  const [categories, setCategories] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [editCat, setEditCat] = useState(null);
  const [formData, setFormData] = useState({ categoryName: '', description: '' });
  const [toast, setToast] = useState(null);

  useEffect(() => { fetchCategories(); }, []);

  const fetchCategories = async () => {
    try {
      const res = await fetch('/api/categories');
      if (res.ok) setCategories(await res.json());
    } catch (err) {}
  };

  const handleSave = async (e) => {
    e.preventDefault();
    const method = editCat ? 'PUT' : 'POST';
    const url = editCat ? `/api/categories/${editCat.categoryId}` : '/api/categories';

    try {
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData)
      });
      if (res.ok) {
        setToast({ type: 'success', message: `Category ${editCat ? 'updated' : 'added'} successfully` });
        setShowModal(false);
        fetchCategories();
      }
    } catch (err) {}
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete category?')) return;
    try {
      const res = await fetch(`/api/categories/${id}`, { method: 'DELETE' });
      if (res.ok) fetchCategories();
    } catch (err) {}
  };

  return (
    <div>
      <div className="header">
        <div>
          <h1 className="page-title">Medicine Categories</h1>
          <p className="subtitle">Classify pharmaceutical products by therapeutic category</p>
        </div>
        <button className="btn btn-primary" onClick={() => { setEditCat(null); setFormData({ categoryName: '', description: '' }); setShowModal(true); }}>
          <Plus size={18} /> Add Category
        </button>
      </div>

      {toast && (
        <div className={`toast toast-${toast.type}`}>
          <span>{toast.message}</span>
          <button style={{ background: 'none', border: 'none', color: 'inherit' }} onClick={() => setToast(null)}>×</button>
        </div>
      )}

      <div className="panel">
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Category Name</th>
              <th>Description</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {categories.map(c => (
              <tr key={c.categoryId}>
                <td>#{c.categoryId}</td>
                <td><strong>{c.categoryName}</strong></td>
                <td>{c.description || 'No description'}</td>
                <td>
                  <button className="btn btn-secondary btn-sm" style={{ marginRight: '0.5rem' }} onClick={() => { setEditCat(c); setFormData({ categoryName: c.categoryName, description: c.description || '' }); setShowModal(true); }}>
                    <Edit2 size={14} />
                  </button>
                  <button className="btn btn-danger btn-sm" onClick={() => handleDelete(c.categoryId)}>
                    <Trash2 size={14} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2>{editCat ? 'Edit Category' : 'Add New Category'}</h2>
            <form onSubmit={handleSave}>
              <div className="form-group">
                <label className="form-label">Category Name</label>
                <input type="text" className="form-control" required value={formData.categoryName} onChange={e => setFormData({ ...formData, categoryName: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="form-label">Description</label>
                <input type="text" className="form-control" value={formData.description} onChange={e => setFormData({ ...formData, description: e.target.value })} />
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save Category</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
