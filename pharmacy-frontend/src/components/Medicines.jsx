import React, { useState, useEffect } from 'react';
import { Search, Plus, Edit2, Trash2 } from 'lucide-react';

export default function Medicines() {
  const [medicines, setMedicines] = useState([]);
  const [categories, setCategories] = useState([]);
  const [search, setSearch] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editMed, setEditMed] = useState(null);
  const [formData, setFormData] = useState({
    medicineName: '',
    categoryId: '',
    price: '',
    stockQuantity: '',
    expiryDate: ''
  });
  const [toast, setToast] = useState(null);

  useEffect(() => {
    fetchMedicines();
    fetchCategories();
  }, []);

  const fetchMedicines = async (query = '') => {
    try {
      const res = await fetch(`/api/medicines${query ? `?search=${encodeURIComponent(query)}` : ''}`);
      if (res.ok) setMedicines(await res.json());
    } catch (err) {
      console.error(err);
    }
  };

  const fetchCategories = async () => {
    try {
      const res = await fetch('/api/categories');
      if (res.ok) setCategories(await res.json());
    } catch (err) {}
  };

  const handleSearch = (e) => {
    setSearch(e.target.value);
    fetchMedicines(e.target.value);
  };

  const openAddModal = () => {
    setEditMed(null);
    setFormData({
      medicineName: '',
      categoryId: categories[0]?.categoryId || '',
      price: '',
      stockQuantity: '',
      expiryDate: ''
    });
    setShowModal(true);
  };

  const openEditModal = (med) => {
    setEditMed(med);
    setFormData({
      medicineName: med.medicineName || '',
      categoryId: med.category?.categoryId || categories[0]?.categoryId || '',
      price: med.price || '',
      stockQuantity: med.stockQuantity || '',
      expiryDate: med.expiryDate || ''
    });
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    const method = editMed ? 'PUT' : 'POST';
    const url = editMed ? `/api/medicines/${editMed.medicineId}` : '/api/medicines';

    const payload = {
      medicineName: formData.medicineName,
      category: { categoryId: formData.categoryId },
      price: parseFloat(formData.price),
      stockQuantity: parseInt(formData.stockQuantity),
      expiryDate: formData.expiryDate || null
    };

    try {
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        setToast({ type: 'success', message: `Medicine ${editMed ? 'updated' : 'added'} successfully!` });
        setShowModal(false);
        fetchMedicines(search);
      } else {
        const err = await res.json();
        setToast({ type: 'error', message: err.message || 'Failed to save medicine' });
      }
    } catch (err) {
      setToast({ type: 'error', message: 'Network error saving medicine' });
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this medicine?')) return;
    try {
      const res = await fetch(`/api/medicines/${id}`, { method: 'DELETE' });
      if (res.ok) {
        setToast({ type: 'success', message: 'Medicine deleted successfully' });
        fetchMedicines(search);
      } else {
        setToast({ type: 'error', message: 'Cannot delete medicine (referenced in existing bills/purchases)' });
      }
    } catch (err) {
      setToast({ type: 'error', message: 'Failed to delete medicine' });
    }
  };

  return (
    <div>
      <div className="header">
        <div>
          <h1 className="page-title">Medicine Management</h1>
          <p className="subtitle">Manage pharmaceutical stock, pricing & expiry alerts</p>
        </div>
        <button className="btn btn-primary" onClick={openAddModal}>
          <Plus size={18} /> Add Medicine
        </button>
      </div>

      {toast && (
        <div className={`toast toast-${toast.type}`}>
          <span>{toast.message}</span>
          <button style={{ background: 'none', border: 'none', color: 'inherit', cursor: 'pointer' }} onClick={() => setToast(null)}>×</button>
        </div>
      )}

      <div className="panel">
        <div className="panel-header">
          <div style={{ position: 'relative', width: '320px' }}>
            <Search size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
            <input
              type="text"
              className="form-control"
              placeholder="Search medicine by name..."
              value={search}
              onChange={handleSearch}
              style={{ paddingLeft: '2.5rem' }}
            />
          </div>
        </div>

        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Medicine Name</th>
              <th>Category</th>
              <th>Selling Price (₹)</th>
              <th>Stock</th>
              <th>Expiry Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {medicines.length === 0 ? (
              <tr><td colSpan="7" style={{ textAlign: 'center', color: '#64748b' }}>No medicines found</td></tr>
            ) : (
              medicines.map(med => (
                <tr key={med.medicineId}>
                  <td>#{med.medicineId}</td>
                  <td><strong>{med.medicineName}</strong></td>
                  <td>{med.category?.categoryName || 'General'}</td>
                  <td>₹{med.price}</td>
                  <td>
                    <span className={`badge ${med.stockQuantity <= 15 ? 'badge-warning' : 'badge-success'}`}>
                      {med.stockQuantity} units
                    </span>
                  </td>
                  <td>{med.expiryDate || 'N/A'}</td>
                  <td>
                    <button className="btn btn-secondary btn-sm" style={{ marginRight: '0.5rem' }} onClick={() => openEditModal(med)}>
                      <Edit2 size={14} />
                    </button>
                    <button className="btn btn-danger btn-sm" onClick={() => handleDelete(med.medicineId)}>
                      <Trash2 size={14} />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h2 style={{ fontFamily: 'var(--font-display)', marginBottom: '1.25rem' }}>
              {editMed ? 'Edit Medicine' : 'Add New Medicine'}
            </h2>
            <form onSubmit={handleSave}>
              <div className="form-group">
                <label className="form-label">Medicine Name</label>
                <input
                  type="text"
                  className="form-control"
                  required
                  value={formData.medicineName}
                  onChange={e => setFormData({ ...formData, medicineName: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Category</label>
                <select
                  className="form-control"
                  required
                  value={formData.categoryId}
                  onChange={e => setFormData({ ...formData, categoryId: e.target.value })}
                >
                  <option value="">Select Category</option>
                  {categories.map(c => <option key={c.categoryId} value={c.categoryId}>{c.categoryName}</option>)}
                </select>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Selling Price (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    className="form-control"
                    required
                    value={formData.price}
                    onChange={e => setFormData({ ...formData, price: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Stock Quantity</label>
                  <input
                    type="number"
                    className="form-control"
                    required
                    value={formData.stockQuantity}
                    onChange={e => setFormData({ ...formData, stockQuantity: e.target.value })}
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Expiry Date</label>
                <input
                  type="date"
                  className="form-control"
                  value={formData.expiryDate}
                  onChange={e => setFormData({ ...formData, expiryDate: e.target.value })}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save Medicine</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
