import React, { useState, useEffect, useCallback } from "react";
import { Search, Plus, Edit2, Trash2, History, RefreshCw, Package, ArrowUpRight, ArrowDownRight, AlertCircle, X } from "lucide-react";
import { api } from "../api/apiClient";
import { useAuth } from "../context/AuthContext";

export default function Medicines() {
  const { isCashier } = useAuth();
  const [activeTab, setActiveTab] = useState("inventory"); // "inventory" or "history"
  const [medicines, setMedicines] = useState([]);
  const [categories, setCategories] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [search, setSearch] = useState("");
  const [historySearch, setHistorySearch] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [editMed, setEditMed] = useState(null);
  const [formData, setFormData] = useState({
    medicineName: "",
    categoryId: "",
    price: "",
    stockQuantity: "",
    expiryDate: ""
  });
  const [toast, setToast] = useState(null);

  const fetchMedicines = useCallback(async (query = "") => {
    try {
      setLoading(true);
      const url = query ? `/api/medicines?search=${encodeURIComponent(query)}` : "/api/medicines";
      const res = await api.get(url);
      if (res.ok) {
        const data = await res.json();
        setMedicines(Array.isArray(data) ? data : []);
      } else {
        setToast({ type: "error", message: "Failed to load medicine inventory." });
      }
    } catch (err) {
      setToast({ type: "error", message: "Network error loading medicines." });
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchCategories = useCallback(async () => {
    try {
      const res = await api.get("/api/categories");
      if (res.ok) {
        const data = await res.json();
        setCategories(Array.isArray(data) ? data : []);
      }
    } catch (err) {
      console.error("Failed to load categories:", err);
    }
  }, []);

  const fetchAuditLogs = useCallback(async () => {
    try {
      setHistoryLoading(true);
      const res = await api.get("/api/reports/audit");
      if (res.ok) {
        const data = await res.json();
        setAuditLogs(Array.isArray(data) ? data : []);
      } else {
        setToast({ type: "error", message: "Failed to load medicine stock history." });
      }
    } catch (err) {
      console.error("Failed to load audit logs:", err);
    } finally {
      setHistoryLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMedicines();
    fetchCategories();
    fetchAuditLogs();
  }, [fetchMedicines, fetchCategories, fetchAuditLogs]);

  const handleSearch = (e) => {
    const val = e.target.value;
    setSearch(val);
    fetchMedicines(val);
  };

  const openAddModal = () => {
    setEditMed(null);
    setFormData({
      medicineName: "",
      categoryId: categories[0]?.categoryId || categories[0]?.id || "",
      price: "",
      stockQuantity: "",
      expiryDate: ""
    });
    setShowModal(true);
  };

  const openEditModal = (med) => {
    setEditMed(med);
    const catId = med.category?.categoryId || med.category?.id || categories[0]?.categoryId || categories[0]?.id || "";
    setFormData({
      medicineName: med.medicineName || "",
      categoryId: catId,
      price: med.price || "",
      stockQuantity: med.stockQuantity || "",
      expiryDate: med.expiryDate || ""
    });
    setShowModal(true);
  };

  const viewMedicineHistory = (med) => {
    setHistorySearch(med.medicineName || "");
    setActiveTab("history");
    fetchAuditLogs();
  };

  const handleSave = async (e) => {
    e.preventDefault();
    const url = editMed ? `/api/medicines/${editMed.medicineId || editMed.id}` : "/api/medicines";

    const payload = {
      medicineName: formData.medicineName.trim(),
      category: { id: formData.categoryId, categoryId: formData.categoryId },
      price: parseFloat(formData.price),
      stockQuantity: parseInt(formData.stockQuantity, 10),
      expiryDate: formData.expiryDate || null
    };

    try {
      const res = editMed ? await api.put(url, payload) : await api.post(url, payload);
      if (res.ok) {
        setToast({ type: "success", message: `Medicine ${editMed ? "updated" : "added"} successfully!` });
        setShowModal(false);
        fetchMedicines(search);
        fetchAuditLogs();
      } else {
        const err = await res.json().catch(() => ({}));
        setToast({ type: "error", message: err.message || "Failed to save medicine" });
      }
    } catch (err) {
      setToast({ type: "error", message: "Network error saving medicine" });
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Are you sure you want to delete this medicine?")) return;
    try {
      const res = await api.delete(`/api/medicines/${id}`);
      if (res.ok) {
        setToast({ type: "success", message: "Medicine deleted successfully" });
        fetchMedicines(search);
        fetchAuditLogs();
      } else {
        setToast({ type: "error", message: "Cannot delete medicine (referenced in existing bills or purchases)" });
      }
    } catch (err) {
      setToast({ type: "error", message: "Failed to delete medicine" });
    }
  };

  // Filter history logs
  const filteredAuditLogs = auditLogs.filter(log => {
    if (!historySearch.trim()) return true;
    const term = historySearch.toLowerCase();
    const medName = (log.medicine?.medicineName || "").toLowerCase();
    const id = (log.auditId || log.id || "").toLowerCase();
    const changeType = (log.changeType || "").toLowerCase();
    return medName.includes(term) || id.includes(term) || changeType.includes(term);
  });

  return (
    <div className="module-container">
      {/* Header */}
      <div className="module-header">
        <div>
          <h2 className="module-title">Medicine Management &amp; Stock History</h2>
          <p className="module-subtitle">Manage pharmaceutical stock, track stock movement audit trail &amp; expiry dates</p>
        </div>

        <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
          {/* Navigation Toggle Tabs */}
          <div style={{ display: "inline-flex", borderRadius: "8px", overflow: "hidden", border: "1px solid var(--border-color, #334155)" }}>
            <button
              type="button"
              className={`btn btn-sm ${activeTab === "inventory" ? "btn-primary" : "btn-secondary"}`}
              style={{ borderRadius: 0, padding: "0.4rem 0.85rem" }}
              onClick={() => setActiveTab("inventory")}
              id="tab-medicines-inventory"
            >
              <Package size={15} style={{ marginRight: "0.35rem" }} /> Medicines Catalog ({medicines.length})
            </button>
            <button
              type="button"
              className={`btn btn-sm ${activeTab === "history" ? "btn-primary" : "btn-secondary"}`}
              style={{ borderRadius: 0, padding: "0.4rem 0.85rem" }}
              onClick={() => { setActiveTab("history"); fetchAuditLogs(); }}
              id="tab-medicines-history"
            >
              <History size={15} style={{ marginRight: "0.35rem" }} /> Stock Movement History ({auditLogs.length})
            </button>
          </div>

          {activeTab === "inventory" && !isCashier && (
            <button className="btn btn-primary btn-sm" onClick={openAddModal} id="btn-add-medicine">
              <Plus size={16} /> Add Medicine
            </button>
          )}

          {activeTab === "history" && (
            <button
              className="btn btn-secondary btn-sm"
              onClick={fetchAuditLogs}
              disabled={historyLoading}
              id="btn-refresh-history"
            >
              <RefreshCw size={15} className={historyLoading ? "spin-animation" : ""} /> Refresh
            </button>
          )}
        </div>
      </div>

      {toast && (
        <div className={`toast toast-${toast.type} mb-4`}>
          <span>{toast.message}</span>
          <button style={{ background: "none", border: "none", color: "inherit", cursor: "pointer", marginLeft: "auto" }} onClick={() => setToast(null)}>×</button>
        </div>
      )}

      {/* TAB 1: MEDICINES INVENTORY CATALOG */}
      {activeTab === "inventory" && (
        <div className="card">
          <div className="filter-bar" style={{ padding: "1rem" }}>
            <div className="search-input-wrapper" style={{ maxWidth: "360px" }}>
              <Search size={16} className="search-icon" />
              <input
                type="text"
                className="form-control"
                placeholder="Search medicine by name..."
                value={search}
                onChange={handleSearch}
                id="search-medicines"
              />
            </div>
          </div>

          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Medicine Name</th>
                  <th>Category</th>
                  <th>Selling Price (₹)</th>
                  <th>Current Stock</th>
                  <th>Expiry Date</th>
                  <th style={{ textAlign: "right" }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="7" className="text-center py-4 text-muted">Loading medicines inventory...</td></tr>
                ) : medicines.length === 0 ? (
                  <tr><td colSpan="7" className="text-center py-4 text-muted">No medicines found matching criteria.</td></tr>
                ) : (
                  medicines.map(med => {
                    const medId = med.medicineId || med.id;
                    const stock = med.stockQuantity || 0;
                    return (
                      <tr key={medId}>
                        <td><span className="code-badge font-mono">#{String(medId).slice(-6)}</span></td>
                        <td><strong>{med.medicineName}</strong></td>
                        <td><span className="badge badge-gray">{med.category?.categoryName || "General"}</span></td>
                        <td className="font-semibold text-emerald">₹{Number(med.price || 0).toFixed(2)}</td>
                        <td>
                          <span className={`badge ${stock <= 15 ? "badge-warning" : "badge-emerald"}`}>
                            {stock} units
                          </span>
                        </td>
                        <td className="text-sm font-mono">{med.expiryDate || "N/A"}</td>
                        <td style={{ textAlign: "right" }}>
                          {/* Medicine History Button */}
                          <button
                            className="btn btn-secondary btn-sm"
                            style={{ marginRight: "0.35rem" }}
                            onClick={() => viewMedicineHistory(med)}
                            title="View Stock Movement History for this medicine"
                          >
                            <History size={13} style={{ marginRight: "0.2rem" }} /> History
                          </button>

                          {!isCashier && (
                            <>
                              <button
                                className="btn btn-secondary btn-sm"
                                style={{ marginRight: "0.35rem" }}
                                onClick={() => openEditModal(med)}
                                title="Edit Medicine Details"
                              >
                                <Edit2 size={13} />
                              </button>
                              <button
                                className="btn btn-danger btn-sm"
                                onClick={() => handleDelete(medId)}
                                title="Delete Medicine"
                              >
                                <Trash2 size={13} />
                              </button>
                            </>
                          )}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 2: MEDICINE STOCK MOVEMENT HISTORY */}
      {activeTab === "history" && (
        <div className="card">
          <div className="filter-bar" style={{ padding: "1rem", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div className="search-input-wrapper" style={{ maxWidth: "360px" }}>
              <Search size={16} className="search-icon" />
              <input
                type="text"
                className="form-control"
                placeholder="Filter by medicine name, audit ID, or change type..."
                value={historySearch}
                onChange={e => setHistorySearch(e.target.value)}
                id="search-history"
              />
            </div>
            {historySearch && (
              <button className="btn btn-secondary btn-sm" onClick={() => setHistorySearch("")}>
                Clear Filter ({historySearch})
              </button>
            )}
          </div>

          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Audit ID</th>
                  <th>Medicine Name</th>
                  <th>Movement Type</th>
                  <th>Qty Change</th>
                  <th>Previous Stock</th>
                  <th>New Stock</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {historyLoading ? (
                  <tr><td colSpan="7" className="text-center py-4 text-muted">Loading stock audit trail...</td></tr>
                ) : filteredAuditLogs.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="text-center py-4 text-muted">
                      {historySearch
                        ? `No stock movement history recorded for "${historySearch}".`
                        : "No medicine stock movement history recorded yet."}
                    </td>
                  </tr>
                ) : (
                  filteredAuditLogs.map(log => {
                    const logId = log.auditId || log.id || "AUD-0";
                    const isReduction = log.changeType === "SALE" || log.changeType === "REDUCE" || (log.quantityChange < 0);
                    return (
                      <tr key={logId}>
                        <td><span className="code-badge font-mono">#{String(logId).slice(-6)}</span></td>
                        <td><strong>{log.medicine?.medicineName || "Unknown Medicine"}</strong></td>
                        <td>
                          <span className={`badge ${isReduction ? "badge-danger" : "badge-emerald"}`}>
                            {isReduction ? <ArrowDownRight size={12} style={{ marginRight: "0.2rem" }} /> : <ArrowUpRight size={12} style={{ marginRight: "0.2rem" }} />}
                            {log.changeType || (isReduction ? "SALE / REDUCTION" : "PURCHASE / STOCK IN")}
                          </span>
                        </td>
                        <td className={`font-bold ${isReduction ? "text-danger" : "text-emerald"}`}>
                          {log.quantityChange > 0 ? `+${log.quantityChange}` : log.quantityChange}
                        </td>
                        <td className="text-muted font-mono">{log.oldQuantity ?? log.oldStock ?? "—"}</td>
                        <td className="font-bold font-mono">{log.newQuantity ?? log.newStock ?? "—"}</td>
                        <td className="text-sm font-mono text-cyan">
                          {log.createdAt || log.changedAt ? new Date(log.createdAt || log.changedAt).toLocaleString() : "Recent"}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add / Edit Medicine Modal */}
      {showModal && (
        <div className="modal-backdrop" onClick={() => setShowModal(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">
                {editMed ? "Edit Medicine" : "Add New Medicine"}
              </h3>
              <button className="btn-icon-close" onClick={() => setShowModal(false)}><X size={18} /></button>
            </div>

            <form onSubmit={handleSave} style={{ padding: "1.25rem" }}>
              <div className="form-group mb-3">
                <label className="form-label">Medicine Name</label>
                <input
                  type="text"
                  className="form-control"
                  required
                  placeholder="e.g. Paracetamol 500mg"
                  value={formData.medicineName}
                  onChange={e => setFormData({ ...formData, medicineName: e.target.value })}
                />
              </div>

              <div className="form-group mb-3">
                <label className="form-label">Category</label>
                <select
                  className="form-control"
                  required
                  value={formData.categoryId}
                  onChange={e => setFormData({ ...formData, categoryId: e.target.value })}
                >
                  <option value="">Select Category</option>
                  {categories.map(c => {
                    const cId = c.categoryId || c.id;
                    return <option key={cId} value={cId}>{c.categoryName}</option>;
                  })}
                </select>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", marginBottom: "1rem" }}>
                <div className="form-group">
                  <label className="form-label">Selling Price (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    className="form-control"
                    required
                    placeholder="0.00"
                    value={formData.price}
                    onChange={e => setFormData({ ...formData, price: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Stock Quantity</label>
                  <input
                    type="number"
                    min="0"
                    className="form-control"
                    required
                    placeholder="0"
                    value={formData.stockQuantity}
                    onChange={e => setFormData({ ...formData, stockQuantity: e.target.value })}
                  />
                </div>
              </div>

              <div className="form-group mb-4">
                <label className="form-label">Expiry Date</label>
                <input
                  type="date"
                  className="form-control"
                  value={formData.expiryDate}
                  onChange={e => setFormData({ ...formData, expiryDate: e.target.value })}
                />
              </div>

              <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.75rem" }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">{editMed ? "Update Medicine" : "Save Medicine"}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
