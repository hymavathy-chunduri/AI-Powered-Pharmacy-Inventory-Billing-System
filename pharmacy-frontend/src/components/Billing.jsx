import React, { useState, useEffect } from 'react';
import { ShoppingCart, Plus, Trash2, CheckCircle2, AlertTriangle, Printer } from 'lucide-react';

export default function Billing() {
  const [customers, setCustomers] = useState([]);
  const [medicines, setMedicines] = useState([]);
  const [selectedCustomer, setSelectedCustomer] = useState('');
  const [paymentMode, setPaymentMode] = useState('CASH');

  const [selectedMedId, setSelectedMedId] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [cart, setCart] = useState([]);

  const [toast, setToast] = useState(null);
  const [completedBill, setCompletedBill] = useState(null);

  useEffect(() => {
    fetchCustomers();
    fetchMedicines();
  }, []);

  const fetchCustomers = async () => {
    try {
      const res = await fetch('/api/customers');
      if (res.ok) {
        const data = await res.json();
        setCustomers(data);
        if (data.length > 0) setSelectedCustomer(data[0].customerId);
      }
    } catch (err) {}
  };

  const fetchMedicines = async () => {
    try {
      const res = await fetch('/api/medicines');
      if (res.ok) {
        const data = await res.json();
        setMedicines(data);
      }
    } catch (err) {}
  };

  const handleAddToCart = () => {
    if (!selectedMedId) {
      setToast({ type: 'error', message: 'Please select a medicine' });
      return;
    }

    const med = medicines.find(m => String(m.medicineId) === String(selectedMedId));
    if (!med) return;

    if (quantity > med.stockQuantity) {
      setToast({
        type: 'error',
        message: `Insufficient stock! Requested: ${quantity}, Available: ${med.stockQuantity}`
      });
      return;
    }

    const existingIdx = cart.findIndex(item => String(item.medicineId) === String(med.medicineId));
    if (existingIdx >= 0) {
      const updatedCart = [...cart];
      const newQty = updatedCart[existingIdx].quantity + parseInt(quantity);
      if (newQty > med.stockQuantity) {
        setToast({
          type: 'error',
          message: `Cannot exceed available stock of ${med.stockQuantity} units`
        });
        return;
      }
      updatedCart[existingIdx].quantity = newQty;
      updatedCart[existingIdx].subtotal = newQty * med.price;
      setCart(updatedCart);
    } else {
      setCart([
        ...cart,
        {
          medicineId: med.medicineId,
          medicineName: med.medicineName,
          unitPrice: med.price,
          quantity: parseInt(quantity),
          subtotal: parseInt(quantity) * med.price,
          availableStock: med.stockQuantity
        }
      ]);
    }

    setToast(null);
  };

  const handleRemoveFromCart = (index) => {
    const updated = cart.filter((_, i) => i !== index);
    setCart(updated);
  };

  const calculateTotal = () => {
    return cart.reduce((acc, item) => acc + item.subtotal, 0);
  };

  const handleSubmitBill = async () => {
    if (!selectedCustomer) {
      setToast({ type: 'error', message: 'Please select a customer' });
      return;
    }

    if (cart.length === 0) {
      setToast({ type: 'error', message: 'Billing cart is empty' });
      return;
    }

    const payload = {
      customerId: selectedCustomer,
      paymentMode: paymentMode,
      items: cart.map(item => ({
        medicineId: item.medicineId,
        quantity: item.quantity
      }))
    };

    try {
      const res = await fetch('/api/bills', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        const savedBill = await res.json();
        setCompletedBill(savedBill);
        setCart([]);
        setToast({ type: 'success', message: 'Bill generated & stock updated successfully!' });
        fetchMedicines(); // Refresh stock
      } else {
        const errorData = await res.json();
        setToast({
          type: 'error',
          message: errorData.message || 'Failed to process bill'
        });
      }
    } catch (err) {
      setToast({ type: 'error', message: 'Network error submitting bill' });
    }
  };

  const selectedMed = medicines.find(m => String(m.medicineId) === String(selectedMedId));

  return (
    <div>
      <div className="header">
        <div>
          <h1 className="page-title">POS Billing Counter</h1>
          <p className="subtitle">Real-time invoice generation with automatic stock reduction</p>
        </div>
      </div>

      {toast && (
        <div className={`toast toast-${toast.type}`}>
          <span>{toast.message}</span>
          <button style={{ background: 'none', border: 'none', color: 'inherit', cursor: 'pointer' }} onClick={() => setToast(null)}>×</button>
        </div>
      )}

      {completedBill && (
        <div className="panel" style={{ border: '1px solid var(--accent-success)', background: 'rgba(16, 185, 129, 0.05)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <CheckCircle2 size={28} color="#10b981" />
              <div>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 600 }}>Bill #{completedBill.billId} Created Successfully</h3>
                <p style={{ color: '#94a3b8', fontSize: '0.85rem' }}>
                  Customer: {completedBill.customer?.customerName} | Total Amount: <strong>₹{completedBill.totalAmount}</strong>
                </p>
              </div>
            </div>
            <button className="btn btn-secondary" onClick={() => window.print()}>
              <Printer size={16} /> Print Receipt
            </button>
          </div>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 360px', gap: '1.5rem' }}>
        <div className="panel">
          <h2 className="panel-title" style={{ marginBottom: '1.25rem' }}>Customer & Items Selection</h2>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
            <div className="form-group">
              <label className="form-label">Select Customer</label>
              <select
                className="form-control"
                value={selectedCustomer}
                onChange={e => setSelectedCustomer(e.target.value)}
              >
                <option value="">-- Choose Customer --</option>
                {customers.map(c => (
                  <option key={c.customerId} value={c.customerId}>
                    {c.customerName} ({c.phone || 'No phone'})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Payment Mode</label>
              <select
                className="form-control"
                value={paymentMode}
                onChange={e => setPaymentMode(e.target.value)}
              >
                <option value="CASH">CASH</option>
                <option value="CARD">CARD</option>
                <option value="UPI">UPI</option>
              </select>
            </div>
          </div>

          <div style={{ padding: '1rem', background: 'rgba(255,255,255,0.03)', borderRadius: 'var(--radius-sm)', marginBottom: '1.5rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr auto', gap: '1rem', alignItems: 'end' }}>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">Select Medicine</label>
                <select
                  className="form-control"
                  value={selectedMedId}
                  onChange={e => setSelectedMedId(e.target.value)}
                >
                  <option value="">-- Choose Medicine --</option>
                  {medicines.map(m => (
                    <option key={m.medicineId} value={m.medicineId}>
                      {m.medicineName} — ₹{m.price} (Stock: {m.stockQuantity})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">Quantity</label>
                <input
                  type="number"
                  min="1"
                  className="form-control"
                  value={quantity}
                  onChange={e => setQuantity(e.target.value)}
                />
              </div>

              <button className="btn btn-primary" onClick={handleAddToCart}>
                <Plus size={16} /> Add Item
              </button>
            </div>

            {selectedMed && (
              <div style={{ marginTop: '0.75rem', fontSize: '0.85rem', color: selectedMed.stockQuantity <= 10 ? '#f59e0b' : '#38bdf8' }}>
                Stock Status: <strong>{selectedMed.stockQuantity} units available</strong>
              </div>
            )}
          </div>

          <table className="data-table">
            <thead>
              <tr>
                <th>Item</th>
                <th>Unit Price (₹)</th>
                <th>Quantity</th>
                <th>Subtotal (₹)</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {cart.length === 0 ? (
                <tr><td colSpan="5" style={{ textAlign: 'center', color: '#64748b' }}>Cart is empty. Add medicines above.</td></tr>
              ) : (
                cart.map((item, idx) => (
                  <tr key={idx}>
                    <td><strong>{item.medicineName}</strong></td>
                    <td>₹{item.unitPrice}</td>
                    <td>{item.quantity}</td>
                    <td>₹{item.subtotal}</td>
                    <td>
                      <button className="btn btn-danger btn-sm" onClick={() => handleRemoveFromCart(idx)}>
                        <Trash2 size={14} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="panel" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <div>
            <h2 className="panel-title" style={{ marginBottom: '1.25rem' }}>Invoice Summary</h2>

            <div style={{ borderBottom: '1px solid var(--border-color)', paddingBottom: '1rem', marginBottom: '1rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem', color: '#94a3b8' }}>
                <span>Subtotal</span>
                <span>₹{calculateTotal()}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem', color: '#94a3b8' }}>
                <span>Tax (0%)</span>
                <span>₹0.00</span>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <span style={{ fontSize: '1.1rem', fontWeight: 600 }}>Total Payable</span>
              <span style={{ fontSize: '1.8rem', fontFamily: 'var(--font-display)', fontWeight: 700, color: '#38bdf8' }}>
                ₹{calculateTotal()}
              </span>
            </div>
          </div>

          <button
            className="btn btn-primary"
            style={{ width: '100%', padding: '1rem', fontSize: '1rem', justifyContent: 'center' }}
            onClick={handleSubmitBill}
          >
            <ShoppingCart size={18} /> Process & Generate Bill
          </button>
        </div>
      </div>
    </div>
  );
}
