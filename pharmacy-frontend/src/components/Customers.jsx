import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2 } from 'lucide-react';

export default function Customers() {
  const [customers, setCustomers] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [editCust, setEditCust] = useState(null);
  const [formData, setFormData] = useState({ customerName: '', phone: '', email: '' });
  const [toast, setToast] = useState(null);

  useEffect(() => { fetchCustomers(); }, []);

  const fetchCustomers = async () => {
    try {
      const res = await fetch('/api/customers');
      if (res.ok) setCustomers(await res.json());
    } catch (err) {}
  };

  const handleSave = async (e) => {
    e.preventDefault();
    const method = editCust ? 'PUT' : 'POST';
    const url = editCust ? `/api/customers/${editCust.customerId}` : '/api/customers';

    try {
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData)
      });
      if (res.ok) {
        setToast({ type: 'success', message: `Customer ${editCust ? 'updated' : 'added'} successfully` });
        setShowModal(false);
        fetchCustomers();
      }
    } catch (err) {}
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete customer?')) return;
    try {
      const res = await fetch(`/api/customers/${id}`, { method: 'DELETE' });
      if (res.ok) fetchCustomers();
    } catch (err) {}
  };

  return (
    <div>
      <div className="header">
        <div>
          <h1 className="page-title">Registered Customers</h1>
          <p className="subtitle">Manage pharmacy customer directory & contact information</p>
        </div>
        <button className="btn btn-primary" onClick={() => { setEditCust(null); setFormData({ customerName: '', phone: '', email: '' }); setShowModal(true); }}>
          <Plus size={18} /> Register Customer
        </button>
      </div>

      {toast && (
        <div className={`toast toast-${toast.type}`}>
          <span>{toast.message}</span>
          <button style={{ background: 'none', border: 'none', color: 'inherit', cursor: 'pointer' }} onClick={() => setToast(null)}>×</button>
        </div>
      )}

      <div className="panel">
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Customer Name</th>
              <th>Phone</th>
              <th>Email</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {customers.map(c => (
              <tr key={c.customerId}>
                <td>#{c.customerId}</td>
                <td><strong>{c.customerName}</strong></td>
                <td>{c.phone || 'N/A'}</td>
                <td>{c.email || 'N/A'}</td>
                <td>
                  <button className="btn btn-secondary btn-sm" style={{ marginRight: '0.5rem' }} onClick={() => { setEditCust(c); setFormData({ customerName: c.customerName, phone: c.phone || '', email: c.email || '' }); setShowModal(true); }}>
                    <Edit2 size={14} />
                  </button>
                  <button className="btn btn-danger btn-sm" onClick={() => handleDelete(c.customerId)}>
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
            <h2>{editCust ? 'Edit Customer' : 'Register New Customer'}</h2>
            <form onSubmit={handleSave}>
              <div className="form-group">
                <label className="form-label">Customer Name</label>
                <input type="text" className="form-control" required value={formData.customerName} onChange={e => setFormData({ ...formData, customerName: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="form-label">Phone Number</label>
                <input type="text" className="form-control" value={formData.phone} onChange={e => setFormData({ ...formData, phone: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="form-label">Email</label>
                <input type="email" className="form-control" value={formData.email} onChange={e => setFormData({ ...formData, email: e.target.value })} />
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save Customer</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
