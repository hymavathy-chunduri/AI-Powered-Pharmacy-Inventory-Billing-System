import React, { useState, useEffect, useCallback } from 'react';
import { Receipt, Search, RefreshCw, Eye, Printer, Calendar, User, CreditCard, CheckCircle2, X } from 'lucide-react';
import { api } from '../api/apiClient';
import { useAuth } from '../context/AuthContext';

export default function MyBills() {
  const { isAdmin, employee } = useAuth();
  const [bills, setBills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchFilter, setSearchFilter] = useState('');
  const [selectedBill, setSelectedBill] = useState(null);

  const fetchBills = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      // Admin fetches /api/bills, employee fetches /api/bills/my
      const url = isAdmin ? '/api/bills' : '/api/bills/my';
      const res = await api.get(url);
      if (res.ok) {
        const data = await res.json();
        setBills(Array.isArray(data) ? data : []);
      } else {
        const err = await res.json().catch(() => ({}));
        setError(err.message || 'Failed to retrieve bills.');
      }
    } catch (err) {
      setError('Network error while loading billing records.');
    } finally {
      setLoading(false);
    }
  }, [isAdmin]);

  useEffect(() => {
    fetchBills();
  }, [fetchBills]);

  const filteredBills = bills.filter(bill => {
    if (!searchFilter.trim()) return true;
    const term = searchFilter.toLowerCase();
    const id = (bill.billId || bill.id || '').toLowerCase();
    const custName = (bill.customer?.customerName || '').toLowerCase();
    const empId = (bill.createdByEmployeeId || '').toLowerCase();
    const empName = (bill.createdByEmployeeName || '').toLowerCase();
    return id.includes(term) || custName.includes(term) || empId.includes(term) || empName.includes(term);
  });

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    try {
      const d = new Date(dateStr);
      return d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
    } catch (e) {
      return dateStr;
    }
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="module-container">
      {/* Module Header */}
      <div className="module-header">
        <div>
          <h2 className="module-title">
            {isAdmin ? 'Pharmacy Sales & Invoices' : 'My Processed Bills'}
          </h2>
          <p className="module-subtitle">
            {isAdmin
              ? 'Complete billing history across all pharmacy cashiers and counters'
              : `Bills and customer invoices issued by ${employee?.fullName || 'Employee'} (${employee?.employeeId || ''})`}
          </p>
        </div>
        <button
          className="btn btn-secondary"
          onClick={fetchBills}
          disabled={loading}
          id="btn-refresh-bills"
        >
          <RefreshCw size={16} className={loading ? 'spin-animation' : ''} /> Refresh
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="filter-bar">
        <div className="search-input-wrapper">
          <Search size={16} className="search-icon" />
          <input
            type="text"
            className="form-control"
            placeholder="Search by Bill ID, Customer Name, or Cashier..."
            value={searchFilter}
            onChange={(e) => setSearchFilter(e.target.value)}
            id="search-bills"
          />
        </div>
      </div>

      {error && (
        <div className="toast toast-error mb-4">
          <span>{error}</span>
        </div>
      )}

      {/* Bills Table */}
      <div className="table-responsive card">
        <table className="table">
          <thead>
            <tr>
              <th>Bill ID</th>
              <th>Date &amp; Time</th>
              <th>Customer</th>
              <th>Items</th>
              <th>Total Amount</th>
              <th>Processed By</th>
              <th style={{ textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="7" className="text-center py-4 text-muted">
                  Loading bills...
                </td>
              </tr>
            ) : filteredBills.length === 0 ? (
              <tr>
                <td colSpan="7" className="text-center py-4 text-muted">
                  No billing records found.
                </td>
              </tr>
            ) : (
              filteredBills.map((b) => {
                const billKey = b.billId || b.id;
                const itemsCount = b.items?.length || 0;
                return (
                  <tr key={billKey}>
                    <td>
                      <span className="code-badge font-mono">#{billKey.substring(0, 8)}...</span>
                    </td>
                    <td className="text-sm font-mono text-cyan">
                      {formatDate(b.billDate)}
                    </td>
                    <td className="font-semibold">
                      {b.customer?.customerName || 'Walk-in Customer'}
                    </td>
                    <td>
                      <span className="badge badge-gray">{itemsCount} item{itemsCount !== 1 ? 's' : ''}</span>
                    </td>
                    <td className="font-bold text-emerald">
                      ₹{Number(b.totalAmount || 0).toFixed(2)}
                    </td>
                    <td className="text-sm">
                      <span className="badge badge-indigo">
                        {b.createdByEmployeeName || b.createdByEmployeeId || 'Counter Staff'}
                      </span>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => setSelectedBill(b)}
                        title="View Full Invoice Receipt"
                      >
                        <Eye size={14} /> View
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Bill Receipt Modal */}
      {selectedBill && (
        <div className="modal-backdrop" onClick={() => setSelectedBill(null)}>
          <div className="modal-card modal-receipt" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Receipt size={20} className="text-emerald" />
                <h3 className="modal-title">Tax Invoice #{selectedBill.billId || selectedBill.id}</h3>
              </div>
              <button
                className="btn-icon-close"
                onClick={() => setSelectedBill(null)}
              >
                <X size={18} />
              </button>
            </div>

            <div className="receipt-content">
              <div className="receipt-meta-grid">
                <div>
                  <span className="receipt-label">Customer Name:</span>
                  <div className="receipt-val">{selectedBill.customer?.customerName || 'Walk-in Customer'}</div>
                  <div className="text-muted text-xs">{selectedBill.customer?.phone || ''}</div>
                </div>
                <div>
                  <span className="receipt-label">Invoice Date:</span>
                  <div className="receipt-val">{formatDate(selectedBill.billDate)}</div>
                </div>
                <div>
                  <span className="receipt-label">Cashier / Staff:</span>
                  <div className="receipt-val">
                    {selectedBill.createdByEmployeeName || selectedBill.createdByEmployeeId} ({selectedBill.createdByEmployeeId})
                  </div>
                </div>
              </div>

              <div className="receipt-items-table mt-4">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Medicine</th>
                      <th style={{ textAlign: 'center' }}>Qty</th>
                      <th style={{ textAlign: 'right' }}>Unit Price</th>
                      <th style={{ textAlign: 'right' }}>Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(selectedBill.items || []).map((item, idx) => (
                      <tr key={idx}>
                        <td className="font-semibold">{item.medicineName || item.medicineId}</td>
                        <td style={{ textAlign: 'center' }}>{item.quantity}</td>
                        <td style={{ textAlign: 'right' }}>₹{Number(item.unitPrice || 0).toFixed(2)}</td>
                        <td style={{ textAlign: 'right' }} className="font-semibold">
                          ₹{Number(item.subtotal || 0).toFixed(2)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot>
                    <tr>
                      <td colSpan="3" style={{ textAlign: 'right', fontWeight: 600 }}>Total Amount:</td>
                      <td style={{ textAlign: 'right', fontWeight: 700, fontSize: '1.1rem' }} className="text-emerald">
                        ₹{Number(selectedBill.totalAmount || 0).toFixed(2)}
                      </td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setSelectedBill(null)}>
                Close
              </button>
              <button className="btn btn-primary" onClick={handlePrint}>
                <Printer size={16} /> Print Receipt
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
