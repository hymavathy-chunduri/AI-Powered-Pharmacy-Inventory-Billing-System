import React, { useState, useEffect } from 'react';
import { Brain, Cpu, TrendingUp, RefreshCw, AlertCircle, CheckCircle } from 'lucide-react';

export default function DemandPrediction() {
  const [metrics, setMetrics] = useState(null);
  const [predictions, setPredictions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchPredictions();
  }, []);

  const fetchPredictions = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch('/api/predictions');
      if (res.ok) {
        const data = await res.json();
        setMetrics(data.metrics);
        setPredictions(data.predictions);
      } else {
        const errData = await res.json();
        setError(errData.message || 'Python ML module offline');
      }
    } catch (err) {
      setError('Unable to reach ML Prediction REST endpoint');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="header">
        <div>
          <h1 className="page-title">AI Demand Prediction & Reorder Advisor</h1>
          <p className="subtitle">Scikit-Learn Machine Learning module for inventory optimization</p>
        </div>
        <button className="btn btn-secondary" onClick={fetchPredictions} disabled={loading}>
          <RefreshCw size={16} className={loading ? 'spin' : ''} /> Run Prediction Model
        </button>
      </div>

      {error && (
        <div className="toast toast-error">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <AlertCircle size={18} />
            <span>{error}. Make sure python script <code>demand_predictor.py</code> is running on port 5001.</span>
          </div>
        </div>
      )}

      {metrics && (
        <div className="panel" style={{ background: 'linear-gradient(135deg, rgba(30, 41, 59, 0.8) 0%, rgba(15, 23, 42, 0.9) 100%)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.25rem' }}>
            <Brain size={28} color="#38bdf8" />
            <div>
              <h2 className="panel-title">Model Comparison & Evaluation Metrics</h2>
              <p style={{ color: '#94a3b8', fontSize: '0.85rem' }}>
                Evaluated on {metrics.sampleCount} historical transactions | Selected Champion Model: <strong>{metrics.bestModel}</strong>
              </p>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
            <div style={{ background: 'rgba(255,255,255,0.03)', padding: '1.25rem', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                <span style={{ fontWeight: 600 }}>Linear Regression</span>
                <span className="badge badge-info">Baseline</span>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.5rem', textAlign: 'center' }}>
                <div>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>MAE</div>
                  <strong style={{ fontSize: '1.1rem' }}>{metrics.linearRegression?.MAE}</strong>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>RMSE</div>
                  <strong style={{ fontSize: '1.1rem' }}>{metrics.linearRegression?.RMSE}</strong>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>R² Score</div>
                  <strong style={{ fontSize: '1.1rem', color: '#38bdf8' }}>{metrics.linearRegression?.R2}</strong>
                </div>
              </div>
            </div>

            <div style={{ background: 'rgba(56, 189, 248, 0.05)', padding: '1.25rem', borderRadius: 'var(--radius-sm)', border: '1px solid rgba(56, 189, 248, 0.3)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                <span style={{ fontWeight: 600, color: '#38bdf8' }}>Random Forest Regressor</span>
                <span className="badge badge-success">Champion</span>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.5rem', textAlign: 'center' }}>
                <div>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>MAE</div>
                  <strong style={{ fontSize: '1.1rem' }}>{metrics.randomForest?.MAE}</strong>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>RMSE</div>
                  <strong style={{ fontSize: '1.1rem' }}>{metrics.randomForest?.RMSE}</strong>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>R² Score</div>
                  <strong style={{ fontSize: '1.1rem', color: '#10b981' }}>{metrics.randomForest?.R2}</strong>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="panel">
        <div className="panel-header">
          <h2 className="panel-title">30-Day Demand Prediction & Reorder Advisory</h2>
          <div style={{ fontSize: '0.85rem', color: '#94a3b8' }}>
            Formula: <code>Reorder Qty = Max(0, Predicted Demand + Safety Buffer (15) - Current Stock)</code>
          </div>
        </div>

        <table className="data-table">
          <thead>
            <tr>
              <th>Medicine</th>
              <th>Current Stock</th>
              <th>Predicted 30-Day Demand</th>
              <th>Safety Buffer</th>
              <th>Recommended Reorder Qty</th>
              <th>Advisory Action</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="6" style={{ textAlign: 'center', color: '#64748b' }}>Running ML models...</td></tr>
            ) : predictions.length === 0 ? (
              <tr><td colSpan="6" style={{ textAlign: 'center', color: '#64748b' }}>No prediction data available</td></tr>
            ) : (
              predictions.map(pred => (
                <tr key={pred.medicineId}>
                  <td><strong>{pred.medicineName}</strong></td>
                  <td>{pred.currentStock} units</td>
                  <td><strong style={{ color: '#38bdf8' }}>{pred.predicted30DayDemand} units</strong></td>
                  <td>+{pred.safetyBuffer} units</td>
                  <td>
                    <span style={{ fontSize: '1.1rem', fontWeight: 700, color: pred.recommendedReorderQuantity > 0 ? '#ef4444' : '#10b981' }}>
                      {pred.recommendedReorderQuantity > 0 ? `${pred.recommendedReorderQuantity} units` : '0 units'}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${pred.status === 'REORDER_REQUIRED' ? 'badge-danger' : 'badge-success'}`}>
                      {pred.status === 'REORDER_REQUIRED' ? 'Reorder Required' : 'Stock Sufficient'}
                    </span>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
