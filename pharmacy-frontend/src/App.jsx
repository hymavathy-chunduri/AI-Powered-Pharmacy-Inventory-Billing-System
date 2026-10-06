import React, { useState } from 'react';
import { Pill, LayoutDashboard, ShoppingBag, Truck, Users, Tag, ShoppingCart, BarChart3, Brain } from 'lucide-react';
import Dashboard from './components/Dashboard';
import Medicines from './components/Medicines';
import Categories from './components/Categories';
import Suppliers from './components/Suppliers';
import Customers from './components/Customers';
import Purchases from './components/Purchases';
import Billing from './components/Billing';
import Reports from './components/Reports';
import DemandPrediction from './components/DemandPrediction';

export default function App() {
  const [activeTab, setActiveTab] = useState('dashboard');

  return (
    <div className="app-container">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon">
            <Pill size={22} color="#ffffff" />
          </div>
          <span>PharmaCare</span>
        </div>

        <ul className="nav-list">
          <li className={`nav-item ${activeTab === 'dashboard' ? 'active' : ''}`} onClick={() => setActiveTab('dashboard')}>
            <LayoutDashboard size={18} /> Dashboard
          </li>
          <li className={`nav-item ${activeTab === 'billing' ? 'active' : ''}`} onClick={() => setActiveTab('billing')}>
            <ShoppingCart size={18} /> POS Billing Counter
          </li>
          <li className={`nav-item ${activeTab === 'medicines' ? 'active' : ''}`} onClick={() => setActiveTab('medicines')}>
            <Pill size={18} /> Medicines Inventory
          </li>
          <li className={`nav-item ${activeTab === 'purchases' ? 'active' : ''}`} onClick={() => setActiveTab('purchases')}>
            <Truck size={18} /> Supplier Purchases
          </li>
          <li className={`nav-item ${activeTab === 'customers' ? 'active' : ''}`} onClick={() => setActiveTab('customers')}>
            <Users size={18} /> Customers
          </li>
          <li className={`nav-item ${activeTab === 'suppliers' ? 'active' : ''}`} onClick={() => setActiveTab('suppliers')}>
            <ShoppingBag size={18} /> Suppliers
          </li>
          <li className={`nav-item ${activeTab === 'categories' ? 'active' : ''}`} onClick={() => setActiveTab('categories')}>
            <Tag size={18} /> Categories
          </li>
          <li className={`nav-item ${activeTab === 'reports' ? 'active' : ''}`} onClick={() => setActiveTab('reports')}>
            <BarChart3 size={18} /> Reports & Audit Log
          </li>
          <li className={`nav-item ${activeTab === 'prediction' ? 'active' : ''}`} onClick={() => setActiveTab('prediction')}>
            <Brain size={18} /> AI Demand Prediction
          </li>
        </ul>
      </aside>

      <main className="main-content">
        {activeTab === 'dashboard' && <Dashboard onNavigate={setActiveTab} />}
        {activeTab === 'billing' && <Billing />}
        {activeTab === 'medicines' && <Medicines />}
        {activeTab === 'purchases' && <Purchases />}
        {activeTab === 'customers' && <Customers />}
        {activeTab === 'suppliers' && <Suppliers />}
        {activeTab === 'categories' && <Categories />}
        {activeTab === 'reports' && <Reports />}
        {activeTab === 'prediction' && <DemandPrediction />}
      </main>
    </div>
  );
}
