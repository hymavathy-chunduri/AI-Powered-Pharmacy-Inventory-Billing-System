# 💊 AI-Powered Pharmacy Inventory & Billing System

A complete **B.Tech CSE Final-Year Project** featuring MongoDB Atlas as the cloud database, Spring Boot REST backend, modern React POS frontend, and Scikit-Learn Python ML Demand Prediction Module.

---

## 🏛️ System Architecture

```text
                            PHARMACY INVENTORY
                            & BILLING SYSTEM
                                   │
                                   ▼
                            ┌─────────────┐
                            │  React POS  │
                            │ Frontend UI │
                            └──────┬──────┘
                                   │
                             REST API (HTTP)
                                   │
                                   ▼
                            ┌─────────────┐
                            │ Spring Boot │
                            │   Backend   │
                            └──────┬──────┘
                                   │
                           Spring Data MongoDB
                                   │
                                   ▼
                            ┌─────────────┐
                            │  MongoDB    │
                            │   Atlas     │
                            │ (Cloud DB)  │
                            └──────┬──────┘
                                   │
                            Historical Sales
                                   │
                                   ▼
                            ┌─────────────┐
                            │  Python ML  │
                            │   Module    │
                            └──────┬──────┘
                                   │
                                   ▼
                          Demand Prediction
                                   │
                                   ▼
                         Reorder Recommendation
```

---

## 🛠️ Technology Stack

| Layer | Technology Used |
| :--- | :--- |
| **Database** | MongoDB Atlas (Cloud), Spring Data MongoDB |
| **Backend** | Java 17, Spring Boot 3.2.4, Spring Data MongoDB, Jakarta Validation |
| **Frontend** | React 18, Vite 5, Lucide Icons, Modern Vanilla CSS |
| **Machine Learning** | Python 3, Scikit-Learn, Pandas, NumPy, Flask, PyMongo |
| **Build Tools** | Maven 3.9, npm |

---

## 🗄️ MongoDB Collections

The system uses the following MongoDB collections:

| Collection | Description |
| :--- | :--- |
| `categories` | Medicine therapeutic categories |
| `suppliers` | Medicine distributors & vendor details |
| `medicines` | Item master: unit prices, stock level, expiry date |
| `customers` | Customer directory & contact records |
| `purchases` | Inbound supplier purchase orders (with embedded items array) |
| `bills` | Customer sales invoices (with embedded items array) |
| `stock_audit` | Automated audit log written by the service layer |
| `users` | System authentication & role management |

### Document Design

- **purchases** and **bills** use **embedded arrays** for their line items (denormalized for read efficiency)
- **medicines** references **categories** via `@DBRef` (independent collections)
- **purchases** references **suppliers** via `@DBRef`
- **bills** references **customers** via `@DBRef`

### Business Logic (replaces PostgreSQL PL/pgSQL triggers)

| Old PostgreSQL Trigger | New MongoDB Implementation |
| :--- | :--- |
| `trg_increase_stock` + `increase_stock()` | `PurchaseService.createPurchase()` — increases `stock_quantity` |
| `trg_reduce_stock` + `reduce_stock()` | `BillingService.createBill()` — decreases `stock_quantity` |
| `trg_stock_audit` + `audit_stock_change()` | Both services write to `stock_audit` collection |
| `trg_update_purchase_total` | `PurchaseService` — calculates total inline |
| `trg_update_bill_total` | `BillingService` — calculates total inline |

---

## 🚀 Getting Started

### 1. Prerequisites

- Java 17+
- Maven 3.9+
- Node.js 18+
- Python 3.10+
- A **MongoDB Atlas** account with a cluster created

### 2. MongoDB Atlas Setup

1. Create a free cluster at [cloud.mongodb.com](https://cloud.mongodb.com)
2. Create a database user with read/write access
3. Add your IP to the IP Access List (or allow `0.0.0.0/0` for development)
4. Copy the connection string from **Connect → Drivers**

### 3. Configure Environment Variables

```bash
# Copy the example file
cp .env.example .env

# Edit .env and fill in your real MongoDB Atlas connection string
# MONGODB_URI=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/
```

### 4. Seed the Database (optional)

```bash
cd database/seed
MONGODB_URI="your-connection-string" python3 seed_mongodb.py
```

### 5. Start Python ML Module

```bash
cd pharmacy-ml
pip install -r requirements.txt
# Use ML_PORT to avoid conflict with Spring Boot (PORT=8080)
ML_PORT=5001 python3 demand_predictor.py
```
*Runs on `http://localhost:5001`*

### 6. Start Spring Boot Backend

```bash
cd pharmacy-backend
MONGODB_URI="your-connection-string" mvn spring-boot:run
```
*Runs on `http://localhost:8080`*

### 7. Start React Frontend

```bash
cd pharmacy-frontend
npm install
npm run dev
```
*Runs on `http://localhost:3000`*

---

## 📡 Core REST API Endpoints

### Medicines API
- `GET /api/medicines` — List all medicines or search (`?search=Paracetamol`)
- `GET /api/medicines/{id}` — Fetch medicine details
- `POST /api/medicines` — Create new medicine
- `PUT /api/medicines/{id}` — Update medicine details
- `DELETE /api/medicines/{id}` — Delete medicine
- `GET /api/medicines/low-stock` — Fetch low-stock alert list (stock ≤ 15)
- `GET /api/medicines/expiry-alerts` — Fetch medicines expiring within 30 days

### Billing & POS API
- `POST /api/bills` — Process customer bill (validates stock, decreases stock, creates audit)
- `GET /api/bills` — Fetch 50 most recent customer bills (paginated for Atlas performance)
- Returns **HTTP 400** with `errorCode: INSUFFICIENT_STOCK` if stock is insufficient

### Purchases API
- `POST /api/purchases` — Record supplier shipment (increases stock, creates audit)
- `GET /api/purchases` — Fetch all purchases

### Reports API
- `GET /api/reports/inventory` — Inventory report (total medicines, low-stock count, total value)
- `GET /api/reports/sales` — Sales report (aggregated from bill items)
- `GET /api/reports/audit` — Stock audit log

### AI Demand Prediction API
- `GET /api/predictions` — ML model metrics + 30-day demand predictions & reorder recommendations

---

## 🤖 AI / Machine Learning Methodology

### Data Source
When `MONGODB_URI` is configured, the ML module reads **live sales data** from the MongoDB `bills` collection (`MONGODB_LIVE` mode — verified: 545 bill-item records loaded). Otherwise, it uses a 1,440-sample `HISTORICAL_ENRICHED` fallback dataset.

### Feature Engineering
Model input features: `medicine_id` (category-encoded), `day_of_week`, `day_of_month`, `month`

### Evaluated Models
1. **Linear Regression**
2. **Random Forest Regressor** (Champion Model — higher R²)

### Model Evaluation Metrics
- **MAE** (Mean Absolute Error)
- **RMSE** (Root Mean Squared Error)
- **R² Score** (Coefficient of Determination)

### Reorder Recommendation Formula
$$\text{Recommended Reorder Qty} = \max\left(0, \text{Predicted 30-Day Demand} + \text{Safety Buffer (15)} - \text{Current Stock}\right)$$

---

## 🔬 Testing & Verification

1. **Backend Tests**: `cd pharmacy-backend && mvn test`  
   Uses embedded Flapdoodle MongoDB — no external database needed.
2. **Stock Validation**: Attempts to bill > available stock return HTTP 400 `INSUFFICIENT_STOCK`
3. **Stock Audit**: All stock changes generate documents in the `stock_audit` collection

---

## 🌐 Production Deployment Configuration

### Environment Variables Matrix

| Service | Variable | Default | Description |
| :--- | :--- | :--- | :--- |
| **Spring Boot** | `MONGODB_URI` | *(required)* | MongoDB Atlas connection string |
| | `MONGODB_DATABASE` | `pharmacy_db` | Database name |
| | `PORT` | `8080` | Backend HTTP port |
| | `ML_SERVICE_URL` | `http://localhost:5001/api/predictions` | ML microservice URL |
| **Python ML** | `MONGODB_URI` | *(optional)* | Atlas URI (falls back to `HISTORICAL_ENRICHED` if absent) |
| | `MONGODB_DATABASE` | `pharmacy_db` | Database name |
| | `ML_PORT` | `5001` | ML Flask port (use `ML_PORT` not `PORT` to avoid conflict with Spring Boot) |
| **React Frontend** | `VITE_API_URL` | `http://localhost:8080` | Spring Boot API base URL |

### Local Development

```bash
# 1. Start Python ML Module
cd pharmacy-ml && MONGODB_URI="your-uri" python3 demand_predictor.py

# 2. Start Spring Boot Backend
cd pharmacy-backend && MONGODB_URI="your-uri" mvn spring-boot:run

# 3. Start React Frontend
cd pharmacy-frontend && npm run dev
```

---

## 🔒 Security Notes

- **No credentials** are committed to Git
- Use `.env` files locally (excluded by `.gitignore`)
- Use environment variables or secrets manager in production
- The browser **never connects directly to MongoDB Atlas** — all database access goes through the Spring Boot REST API
