import React, { useState, useEffect } from 'react';
import {
  Pill,
  LayoutDashboard,
  ShoppingBag,
  Truck,
  Users,
  Tag,
  ShoppingCart,
  BarChart3,
  Brain,
  ShieldCheck,
  LogOut,
  UserCheck,
  Sparkles,
  Loader2,
  Receipt,
  Activity,
  History
} from 'lucide-react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import Medicines from './components/Medicines';
import Categories from './components/Categories';
import Suppliers from './components/Suppliers';
import Customers from './components/Customers';
import Purchases from './components/Purchases';
import Billing from './components/Billing';
import Reports from './components/Reports';
import DemandPrediction from './components/DemandPrediction';
import Employees from './components/Employees';
import MyBills from './components/MyBills';
import EmployeeActivity from './components/EmployeeActivity';
import LoginHistory from './components/LoginHistory';

function MainApp() {
  const { employee, loading, logout, isAdmin, isPharmacist, isCashier, isAuthenticated } = useAuth();
  const [activeTab, setActiveTab] = useState('dashboard');

  // If role changes or is restricted, return to dashboard
  useEffect(() => {
    if (isCashier && ['purchases', 'suppliers', 'categories', 'reports', 'prediction', 'employees'].includes(activeTab)) {
      setActiveTab('dashboard');
    } else if (isPharmacist && activeTab === 'employees') {
      setActiveTab('dashboard');
    }
  }, [activeTab, isCashier, isPharmacist]);

  if (loading) {
    return (
      <div className="loading-screen">
        <div className="loading-content">
          <div className="loading-logo">
            <Pill size={40} className="pulse-icon" />
          </div>
          <h2>PharmaCare</h2>
          <p className="text-muted">Authenticating employee session...</p>
          <Loader2 size={24} className="spin-animation mt-3 text-cyan" />
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Login onSuccess={() => setActiveTab('dashboard')} />;
  }

  const getRoleBadgeStyle = (role) => {
    switch (role) {
      case 'ADMIN':
        return { bg: 'rgba(129, 140, 248, 0.2)', color: '#a5b4fc', border: '1px solid rgba(129, 140, 248, 0.4)' };
      case 'PHARMACIST':
        return { bg: 'rgba(16, 185, 129, 0.2)', color: '#6ee7b7', border: '1px solid rgba(16, 185, 129, 0.4)' };
      case 'CASHIER':
        return { bg: 'rgba(56, 189, 248, 0.2)', color: '#7dd3fc', border: '1px solid rgba(56, 189, 248, 0.4)' };
      default:
        return { bg: 'rgba(148, 163, 184, 0.2)', color: '#cbd5e1', border: '1px solid rgba(148, 163, 184, 0.4)' };
    }
  };

  const getTabTitle = (tab) => {
    switch (tab) {
      case 'my-bills': return 'My Bills';
      case 'activity': return 'Employee Activity';
      case 'login-history': return 'Login History';
      case 'prediction': return 'AI Demand Prediction';
      default: return tab.charAt(0).toUpperCase() + tab.slice(1);
    }
  };

  const badgeStyle = getRoleBadgeStyle(employee?.role);

  return (
    <div className="app-container">
      {/* Sidebar Navigation */}
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon">
            <Pill size={22} color="#ffffff" />
          </div>
          <span>PharmaCare</span>
        </div>

        <ul className="nav-list">
          <li
            className={`nav-item ${activeTab === 'dashboard' ? 'active' : ''}`}
            onClick={() => setActiveTab('dashboard')}
            id="nav-dashboard"
          >
            <LayoutDashboard size={18} /> Dashboard
          </li>

          <li
            className={`nav-item ${activeTab === 'billing' ? 'active' : ''}`}
            onClick={() => setActiveTab('billing')}
            id="nav-billing"
          >
            <ShoppingCart size={18} /> POS Billing Counter
          </li>

          <li
            className={`nav-item ${activeTab === 'my-bills' ? 'active' : ''}`}
            onClick={() => setActiveTab('my-bills')}
            id="nav-my-bills"
          >
            <Receipt size={18} /> {isAdmin ? 'All Invoices' : 'My Bills'}
          </li>

          <li
            className={`nav-item ${activeTab === 'activity' ? 'active' : ''}`}
            onClick={() => setActiveTab('activity')}
            id="nav-activity"
          >
            <Activity size={18} /> {isAdmin ? 'Staff Performance' : 'My Activity'}
          </li>

          <li
            className={`nav-item ${activeTab === 'login-history' ? 'active' : ''}`}
            onClick={() => setActiveTab('login-history')}
            id="nav-login-history"
          >
            <History size={18} /> Login History
          </li>

          <li
            className={`nav-item ${activeTab === 'medicines' ? 'active' : ''}`}
            onClick={() => setActiveTab('medicines')}
            id="nav-medicines"
          >
            <Pill size={18} /> Medicines Inventory
          </li>

          {/* Supplier Purchases: Admin and Pharmacist only */}
          {!isCashier && (
            <li
              className={`nav-item ${activeTab === 'purchases' ? 'active' : ''}`}
              onClick={() => setActiveTab('purchases')}
              id="nav-purchases"
            >
              <Truck size={18} /> Supplier Purchases
            </li>
          )}

          <li
            className={`nav-item ${activeTab === 'customers' ? 'active' : ''}`}
            onClick={() => setActiveTab('customers')}
            id="nav-customers"
          >
            <Users size={18} /> Customers
          </li>

          {/* Suppliers: Admin and Pharmacist only */}
          {!isCashier && (
            <li
              className={`nav-item ${activeTab === 'suppliers' ? 'active' : ''}`}
              onClick={() => setActiveTab('suppliers')}
              id="nav-suppliers"
            >
              <ShoppingBag size={18} /> Suppliers
            </li>
          )}

          {/* Categories: Admin and Pharmacist only */}
          {!isCashier && (
            <li
              className={`nav-item ${activeTab === 'categories' ? 'active' : ''}`}
              onClick={() => setActiveTab('categories')}
              id="nav-categories"
            >
              <Tag size={18} /> Categories
            </li>
          )}

          {/* Reports: Admin and Pharmacist only */}
          {!isCashier && (
            <li
              className={`nav-item ${activeTab === 'reports' ? 'active' : ''}`}
              onClick={() => setActiveTab('reports')}
              id="nav-reports"
            >
              <BarChart3 size={18} /> Reports &amp; Audit Log
            </li>
          )}

          {/* AI Demand Prediction: Admin and Pharmacist */}
          {!isCashier && (
            <li
              className={`nav-item ${activeTab === 'prediction' ? 'active' : ''}`}
              onClick={() => setActiveTab('prediction')}
              id="nav-prediction"
            >
              <Brain size={18} /> AI Demand Prediction
            </li>
          )}

          {/* Employee Management: Admin only */}
          {isAdmin && (
            <li
              className={`nav-item ${activeTab === 'employees' ? 'active' : ''}`}
              onClick={() => setActiveTab('employees')}
              id="nav-employees"
            >
              <ShieldCheck size={18} /> Employee Management
            </li>
          )}
        </ul>

        {/* Sidebar employee compact status */}
        <div className="sidebar-footer">
          <div className="sidebar-employee-card">
            <div className="sidebar-avatar">
              <UserCheck size={18} />
            </div>
            <div className="sidebar-user-details">
              <div className="sidebar-user-name" title={employee?.fullName}>
                {employee?.fullName}
              </div>
              <div className="sidebar-user-id text-muted">
                {employee?.employeeId}
              </div>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="main-wrapper">
        {/* Top Header with Employee Info & Logout Button */}
        <header className="top-header">
          <div className="header-breadcrumbs">
            <span className="system-pill">PharmaCare System</span>
            <span className="breadcrumb-separator">/</span>
            <span className="active-module-title">
              {getTabTitle(activeTab)}
            </span>
          </div>

          <div className="header-right-actions">
            {/* Employee Profile Pill */}
            <div className="employee-pill">
              <div className="employee-details">
                <span className="employee-name">{employee?.fullName}</span>
                <span className="employee-id-tag">({employee?.employeeId})</span>
              </div>
              <span
                className="role-badge"
                style={{
                  backgroundColor: badgeStyle.bg,
                  color: badgeStyle.color,
                  border: badgeStyle.border,
                }}
              >
                {employee?.role}
              </span>
            </div>

            {/* Logout Button */}
            <button
              onClick={logout}
              className="btn-logout"
              title="Sign Out of Employee Session"
              id="btn-logout"
            >
              <LogOut size={16} />
              <span>Log Out</span>
            </button>
          </div>
        </header>

        {/* Module Content */}
        <main className="main-content">
          {activeTab === 'dashboard' && <Dashboard onNavigate={setActiveTab} />}
          {activeTab === 'billing' && <Billing />}
          {activeTab === 'my-bills' && <MyBills />}
          {activeTab === 'activity' && <EmployeeActivity />}
          {activeTab === 'login-history' && <LoginHistory />}
          {activeTab === 'medicines' && <Medicines />}
          {activeTab === 'purchases' && !isCashier && <Purchases />}
          {activeTab === 'customers' && <Customers />}
          {activeTab === 'suppliers' && !isCashier && <Suppliers />}
          {activeTab === 'categories' && !isCashier && <Categories />}
          {activeTab === 'reports' && !isCashier && <Reports />}
          {activeTab === 'prediction' && !isCashier && <DemandPrediction />}
          {activeTab === 'employees' && isAdmin && <Employees />}
        </main>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}
