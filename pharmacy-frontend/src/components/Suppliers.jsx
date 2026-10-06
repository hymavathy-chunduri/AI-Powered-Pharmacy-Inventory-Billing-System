import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2 } from 'lucide-react';

export default function Suppliers() {
  const [suppliers, setSuppliers] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [editSup, setEditSup] = useState(null);
  const [formData, setFormData] = useState({ supplierName: '', contactName: '', phone: '', email: '', address: '' });
  const [toast, setToast] = useState(null);

  useEffect(() => { fetchSuppliers(); }, []);

  const fetchSuppliers = async () => {
    try {
      const res = await fetch('/api/suppliers');
      if (res.ok) setSuppliers(await res.json());
    } catch (err) {}
  };

  const handleSave = async (e) => {
    e.preventDefault();
    const method = editSup ? 'PUT' : 'POST';
    const url = editSup ? `/api/suppliers/${editSup.supplierId}` : '/api/suppliers';

    try {
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData)
      });
      if (res.ok) {
        setToast({ type: 'success', message: `Supplier ${editSup ? 'updated' : 'added'} successfully` });
        setShowModal(false);
        fetchSuppliers();
      }
    } catch (err) {}
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete supplier?')) return;
    try {
      const res = await fetch(`/api/suppliers/${id}`, { method: 'DELETE' });
      if (res.ok) fetchSuppliers();
    } catch (err) {}
  };

  return (
    <div>
      <div className="header">
        <div>
          <h1 className="page-title">Supplier Management</h1>
          <p className="subtitle">Manage distributors and pharmaceutical vendors</p>
        </div>
        <button className="btn btn-primary" onClick={() => { setEditSup(null); setFormData({ supplierName: '', contactName: '', phone: '', email: '', address: '' }); setShowModal(true); }}>
          <Plus size={18} /> Add Supplier
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
              <th>Supplier Name</th>
              <th>Contact Person</th>
              <th>Phone</th>
              <th>Email</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {suppliers.map(s => (
              <tr key={s.supplierId}>
                <td>#{s.supplierId}</td>
                <td><strong>{s.supplierName}</strong></td>
                <td>{s.contactName || 'N/A'}</td>
                <td>{s.phone || 'N/A'}</td>
                <td>{s.email || 'N/A'}</td>
                <td>
                  <button className="btn btn-secondary btn-sm" style={{ marginRight: '0.5rem' }} onClick={() => { setEditSup(s); setFormData({ supplierName: s.supplierName, contactName: s.contactName || '', phone: s.phone || '', email: s.email || '', address: s.address || '' }); setShowModal(true); }}>
                    <Edit2 size={14} />
                  </button>
                  <button className="btn btn-danger btn-sm" onClick={() => handleDelete(s.supplierId)}>
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
            <h2>{editSup ? 'Edit Supplier' : 'Add New Supplier'}</h2>
            <form onSubmit={handleSave}>
              <div className="form-group">
                <label className="form-label">Supplier Company Name</label>
                <input type="text" className="form-control" required value={formData.supplierName} onChange={e => setFormData({ ...formData, supplierName: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="form-label">Contact Representative</label>
                <input type="text" className="form-control" value={formData.contactName} onChange={e => setFormData({ ...formData, contactName: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="form-label">Phone</label>
                <input type="text" className="form-control" value={formData.phone} onChange={e => setFormData({ ...formData, phone: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="form-label">Email</label>
                <input type="email" className="form-control" value={formData.email} onChange={e => setFormData({ ...formData, email: e.target.value })} />
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save Supplier</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
