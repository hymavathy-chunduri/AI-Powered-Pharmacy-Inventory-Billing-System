import React, { useState, useEffect } from 'react';
import { Truck, Plus, CheckCircle2 } from 'lucide-react';

export default function Purchases() {
  const [purchases, setPurchases] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [medicines, setMedicines] = useState([]);

  const [selectedSupplier, setSelectedSupplier] = useState('');
  const [selectedMedId, setSelectedMedId] = useState('');
  const [quantity, setQuantity] = useState(10);
  const [unitPrice, setUnitPrice] = useState('');

  const [toast, setToast] = useState(null);

  useEffect(() => {
    fetchPurchases();
    fetchSuppliers();
    fetchMedicines();
  }, []);

  const fetchPurchases = async () => {
    try {
      const res = await fetch('/api/purchases');
      if (res.ok) setPurchases(await res.json());
    } catch (err) {}
  };

  const fetchSuppliers = async () => {
    try {
      const res = await fetch('/api/suppliers');
      if (res.ok) setSuppliers(await res.json());
    } catch (err) {}
  };

  const fetchMedicines = async () => {
    try {
      const res = await fetch('/api/medicines');
      if (res.ok) setMedicines(await res.json());
    } catch (err) {}
  };

  const handleCreatePurchase = async (e) => {
    e.preventDefault();
    if (!selectedSupplier || !selectedMedId || !quantity || !unitPrice) {
      setToast({ type: 'error', message: 'Please fill in all purchase details' });
      return;
    }

    const payload = {
      supplierId: selectedSupplier,
      items: [
        {
          medicineId: selectedMedId,
          quantity: parseInt(quantity),
          unitPrice: parseFloat(unitPrice)
        }
      ]
    };

    try {
      const res = await fetch('/api/purchases', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        setToast({ type: 'success', message: 'Purchase recorded & medicine stock increased in database!' });
        fetchPurchases();
        fetchMedicines();
      } else {
        setToast({ type: 'error', message: 'Failed to record purchase' });
      }
    } catch (err) {
      setToast({ type: 'error', message: 'Network error recording purchase' });
    }
  };

  return (
    <div>
      <div className="header">
        <div>
          <h1 className="page-title">Supplier Purchases</h1>
          <p className="subtitle">Record inbound stock shipments from verified suppliers</p>
        </div>
      </div>

      {toast && (
        <div className={`toast toast-${toast.type}`}>
          <span>{toast.message}</span>
          <button style={{ background: 'none', border: 'none', color: 'inherit', cursor: 'pointer' }} onClick={() => setToast(null)}>×</button>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '380px 1fr', gap: '1.5rem' }}>
        <div className="panel">
          <h2 className="panel-title" style={{ marginBottom: '1.25rem' }}>Record Inbound Shipment</h2>
          <form onSubmit={handleCreatePurchase}>
            <div className="form-group">
              <label className="form-label">Supplier</label>
              <select
                className="form-control"
                required
                value={selectedSupplier}
                onChange={e => setSelectedSupplier(e.target.value)}
              >
                <option value="">-- Choose Supplier --</option>
                {suppliers.map(s => <option key={s.supplierId} value={s.supplierId}>{s.supplierName}</option>)}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Medicine</label>
              <select
                className="form-control"
                required
                value={selectedMedId}
                onChange={e => {
                  setSelectedMedId(e.target.value);
                  const m = medicines.find(med => String(med.medicineId) === String(e.target.value));
                  if (m) setUnitPrice(m.purchasePrice || m.price);
                }}
              >
                <option value="">-- Choose Medicine --</option>
                {medicines.map(m => <option key={m.medicineId} value={m.medicineId}>{m.medicineName} (Stock: {m.stockQuantity})</option>)}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Purchased Quantity</label>
              <input
                type="number"
                min="1"
                className="form-control"
                required
                value={quantity}
                onChange={e => setQuantity(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Unit Cost Price (₹)</label>
              <input
                type="number"
                step="0.01"
                className="form-control"
                required
                value={unitPrice}
                onChange={e => setUnitPrice(e.target.value)}
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '1rem', justifyContent: 'center' }}>
              <Truck size={16} /> Save & Increase Stock
            </button>
          </form>
        </div>

        <div className="panel">
          <h2 className="panel-title" style={{ marginBottom: '1.25rem' }}>Recent Purchase Orders</h2>
          <table className="data-table">
            <thead>
              <tr>
                <th>Purchase #</th>
                <th>Supplier</th>
                <th>Date</th>
                <th>Total Amount (₹)</th>
              </tr>
            </thead>
            <tbody>
              {purchases.length === 0 ? (
                <tr><td colSpan="4" style={{ textAlign: 'center', color: '#64748b' }}>No purchases recorded</td></tr>
              ) : (
                purchases.map(p => (
                  <tr key={p.purchaseId}>
                    <td>#{p.purchaseId}</td>
                    <td>{p.supplier?.supplierName || 'Standard Supplier'}</td>
                    <td>{p.purchaseDate ? new Date(p.purchaseDate).toLocaleDateString() : 'Today'}</td>
                    <td><strong>₹{p.totalAmount}</strong></td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
