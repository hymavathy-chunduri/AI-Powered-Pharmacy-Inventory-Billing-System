import React, { useState, useEffect } from 'react';
import { Pill, AlertTriangle, Clock, DollarSign, Users, TrendingUp } from 'lucide-react';

export default function Dashboard({ onNavigate }) {
  const [stats, setStats] = useState({
    totalMedicines: 0,
    lowStockCount: 0,
    expiryAlertCount: 0,
    totalCustomers: 0,
    totalBills: 0
  });
  const [lowStockList, setLowStockList] = useState([]);
  const [recentBills, setRecentBills] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const [medsRes, lowStockRes, expiryRes, custRes, billsRes] = await Promise.all([
        fetch('/api/medicines').then(r => r.ok ? r.json() : []),
        fetch('/api/medicines/low-stock').then(r => r.ok ? r.json() : []),
        fetch('/api/medicines/expiry-alerts').then(r => r.ok ? r.json() : []),
        fetch('/api/customers').then(r => r.ok ? r.json() : []),
        fetch('/api/bills').then(r => r.ok ? r.json() : [])
      ]);

      setStats({
        totalMedicines: medsRes.length,
        lowStockCount: lowStockRes.length,
        expiryAlertCount: expiryRes.length,
        totalCustomers: custRes.length,
        totalBills: billsRes.length
      });

      setLowStockList(lowStockRes.slice(0, 5));
      setRecentBills(billsRes.slice(-5).reverse());
    } catch (err) {
      console.error('Error fetching dashboard data:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="header">
        <div>
          <h1 className="page-title">Pharmacy Dashboard</h1>
          <p className="subtitle">Real-time inventory overview, stock alerts & billing metrics</p>
        </div>
        <button className="btn btn-primary" onClick={() => onNavigate('billing')}>
          + Create New Bill
        </button>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(56, 189, 248, 0.15)', color: '#38bdf8' }}>
            <Pill size={24} />
          </div>
          <div className="stat-info">
            <h3>{stats.totalMedicines}</h3>
            <p>Total Medicines</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(245, 158, 11, 0.15)', color: '#f59e0b' }}>
            <AlertTriangle size={24} />
          </div>
          <div className="stat-info">
            <h3>{stats.lowStockCount}</h3>
            <p>Low Stock Items</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(239, 68, 68, 0.15)', color: '#ef4444' }}>
            <Clock size={24} />
          </div>
          <div className="stat-info">
            <h3>{stats.expiryAlertCount}</h3>
            <p>Expiring Soon</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.15)', color: '#10b981' }}>
            <Users size={24} />
          </div>
          <div className="stat-info">
            <h3>{stats.totalCustomers}</h3>
            <p>Total Customers</p>
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
        <div className="panel">
          <div className="panel-header">
            <h2 className="panel-title">Low Stock Alert</h2>
            <button className="btn btn-secondary btn-sm" onClick={() => onNavigate('medicines')}>View All</button>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                <th>Medicine</th>
                <th>Current Stock</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {lowStockList.length === 0 ? (
                <tr><td colSpan="3" style={{ textAlign: 'center', color: '#64748b' }}>No low stock alerts</td></tr>
              ) : (
                lowStockList.map(med => (
                  <tr key={med.medicineId}>
                    <td>{med.medicineName}</td>
                    <td><strong>{med.stockQuantity}</strong> units</td>
                    <td>
                      <span className="badge badge-warning">Low Stock</span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="panel">
          <div className="panel-header">
            <h2 className="panel-title">Recent Customer Bills</h2>
            <button className="btn btn-secondary btn-sm" onClick={() => onNavigate('reports')}>Sales History</button>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                <th>Bill #</th>
                <th>Customer</th>
                <th>Total (₹)</th>
                <th>Payment</th>
              </tr>
            </thead>
            <tbody>
              {recentBills.length === 0 ? (
                <tr><td colSpan="4" style={{ textAlign: 'center', color: '#64748b' }}>No recent bills recorded</td></tr>
              ) : (
                recentBills.map(bill => (
                  <tr key={bill.billId}>
                    <td>#{bill.billId}</td>
                    <td>{bill.customer ? bill.customer.customerName : 'Walk-in'}</td>
                    <td><strong>₹{bill.totalAmount}</strong></td>
                    <td>
                      <span className="badge badge-success">{bill.paymentMode || 'CASH'}</span>
                    </td>
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
