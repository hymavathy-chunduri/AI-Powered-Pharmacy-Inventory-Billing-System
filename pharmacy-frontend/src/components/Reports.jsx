import React, { useState, useEffect } from 'react';
import { FileText, ShieldCheck, DollarSign, Package } from 'lucide-react';

export default function Reports() {
  const [activeTab, setActiveTab] = useState('inventory');
  const [inventoryReport, setInventoryReport] = useState({ totalMedicines: 0, lowStockCount: 0, totalInventoryValue: 0, medicines: [] });
  const [auditLogs, setAuditLogs] = useState([]);
  const [salesReport, setSalesReport] = useState([]);

  useEffect(() => {
    fetchReports();
  }, []);

  const fetchReports = async () => {
    try {
      const [invRes, auditRes, salesRes] = await Promise.all([
        fetch('/api/reports/inventory').then(r => r.ok ? r.json() : {}),
        fetch('/api/reports/audit').then(r => r.ok ? r.json() : []),
        fetch('/api/reports/sales').then(r => r.ok ? r.json() : [])
      ]);

      setInventoryReport(invRes);
      setAuditLogs(auditRes);
      setSalesReport(salesRes);
    } catch (err) {}
  };

  return (
    <div>
      <div className="header">
        <div>
          <h1 className="page-title">Analytics & Audit Reports</h1>
          <p className="subtitle">Database views, sales summaries & PostgreSQL trigger audit trail</p>
        </div>
      </div>

      <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem' }}>
        <button
          className={`btn ${activeTab === 'inventory' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('inventory')}
        >
          <Package size={16} /> Inventory Valuation
        </button>
        <button
          className={`btn ${activeTab === 'audit' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('audit')}
        >
          <ShieldCheck size={16} /> Stock Audit Log (Triggers)
        </button>
        <button
          className={`btn ${activeTab === 'sales' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('sales')}
        >
          <DollarSign size={16} /> Sales Summary
        </button>
      </div>

      {activeTab === 'inventory' && (
        <div className="panel">
          <div className="stats-grid" style={{ marginBottom: '1.5rem' }}>
            <div className="stat-card">
              <div className="stat-info">
                <h3>{inventoryReport.totalMedicines || 0}</h3>
                <p>Total Items in Stock</p>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-info">
                <h3 style={{ color: '#38bdf8' }}>₹{inventoryReport.totalInventoryValue || 0}</h3>
                <p>Total Asset Valuation</p>
              </div>
            </div>
          </div>

          <table className="data-table">
            <thead>
              <tr>
                <th>Medicine</th>
                <th>Category</th>
                <th>Selling Price</th>
                <th>Quantity</th>
                <th>Total Value (₹)</th>
              </tr>
            </thead>
            <tbody>
              {(inventoryReport.medicines || []).map(m => (
                <tr key={m.medicineId}>
                  <td><strong>{m.medicineName}</strong></td>
                  <td>{m.category?.categoryName || 'General'}</td>
                  <td>₹{m.price}</td>
                  <td>{m.stockQuantity} units</td>
                  <td>₹{(m.price * m.stockQuantity).toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === 'audit' && (
        <div className="panel">
          <h2 className="panel-title" style={{ marginBottom: '1.25rem' }}>PostgreSQL Stock Audit Trail (trg_stock_audit)</h2>
          <table className="data-table">
            <thead>
              <tr>
                <th>Audit ID</th>
                <th>Medicine</th>
                <th>Change Type</th>
                <th>Qty Change</th>
                <th>Old Qty</th>
                <th>New Qty</th>
                <th>Timestamp</th>
              </tr>
            </thead>
            <tbody>
              {auditLogs.length === 0 ? (
                <tr><td colSpan="7" style={{ textAlign: 'center', color: '#64748b' }}>No audit records recorded yet</td></tr>
              ) : (
                auditLogs.map(log => (
                  <tr key={log.auditId}>
                    <td>#{log.auditId}</td>
                    <td><strong>{log.medicine?.medicineName || 'Medicine'}</strong></td>
                    <td>
                      <span className={`badge ${log.changeType === 'REDUCE' || log.changeType === 'SALE' ? 'badge-danger' : 'badge-success'}`}>
                        {log.changeType}
                      </span>
                    </td>
                    <td>{log.quantityChange > 0 ? `+${log.quantityChange}` : log.quantityChange}</td>
                    <td>{log.oldQuantity}</td>
                    <td><strong>{log.newQuantity}</strong></td>
                    <td>{log.createdAt ? new Date(log.createdAt).toLocaleString() : 'Recent'}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === 'sales' && (
        <div className="panel">
          <h2 className="panel-title" style={{ marginBottom: '1.25rem' }}>Sales Report (sales_report View)</h2>
          <table className="data-table">
            <thead>
              <tr>
                <th>Bill ID</th>
                <th>Customer</th>
                <th>Bill Date</th>
                <th>Total Revenue (₹)</th>
              </tr>
            </thead>
            <tbody>
              {salesReport.length === 0 ? (
                <tr><td colSpan="4" style={{ textAlign: 'center', color: '#64748b' }}>No sales history data available</td></tr>
              ) : (
                salesReport.map((row, idx) => (
                  <tr key={idx}>
                    <td>#{row.bill_id || row.billId}</td>
                    <td>{row.customer_name || row.customerName || 'Customer'}</td>
                    <td>{row.bill_date || 'Date'}</td>
                    <td><strong>₹{row.total_amount || row.totalAmount}</strong></td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
