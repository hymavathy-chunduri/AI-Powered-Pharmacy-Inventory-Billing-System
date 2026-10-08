# Project Documentation — AI-Powered Pharmacy Inventory & Billing System

## Final Architecture

```
React Frontend  (Vite + React 18)
       │
       │  REST API over HTTP (CORS enabled)
       ▼
Spring Boot Backend  (Java 17+, Spring Boot 3.2.4, Spring Data MongoDB)
       │
       │  Spring Data MongoDB / MongoTemplate (SSL/TLS)
       ▼
MongoDB Atlas  (M0 Free Tier, pharmacy_db, 8 collections)
       │
       │  PyMongo direct read (historical bills)
       ▼
Python ML Service  (Flask + Scikit-Learn, port 5001)
       │
       ▼
Demand Predictions  → Spring Boot → React Frontend
```

---

## Database: MongoDB Atlas

- **Cluster**: `pharmacy-cluster` (M0 free tier, AWS ap-south-1)
- **Database**: `pharmacy_db`
- **MongoDB version**: 8.0.34

### Collections

| Collection    | Documents | Description |
|:------------- |----------:|:----------- |
| categories    | 5  | Therapeutic categories |
| suppliers     | 3  | Medicine distributors |
| medicines     | 8  | Item master + stock + expiry |
| customers     | 4  | Customer directory |
| purchases     | 3+ | Inbound orders (embedded items) |
| bills         | 270 | Customer invoices (embedded items) |
| stock_audit   | 10+ | Automated stock change log |
| users         | 2  | Admin + staff (dev-only demo credentials) |

### Document Design

- `purchases` and `bills` use **embedded arrays** for line items (denormalized)
- `medicines → categories`: `@DBRef` (independent collections)
- `purchases → suppliers`: `@DBRef`
- `bills → customers`: `@DBRef`

### Date Fields
All `expiry_date` and `manufacture_date` fields are stored as **BSON ISODate** (MongoDB `datetime`), not strings. This enables correct `findByExpiryDateBefore(LocalDate)` queries.

---

## Backend: Spring Boot

- **Port**: 8080
- **Java**: OpenJDK 17+ (tested on OpenJDK 27)
- **Connection**: `spring.data.mongodb.uri` from `MONGODB_URI` environment variable
- **Database name**: `spring.data.mongodb.database` from `MONGODB_DATABASE` (default: `pharmacy_db`)

### Business Logic (replaces PostgreSQL triggers)

| Old PostgreSQL Trigger | Java Service Implementation |
|:----------------------|:---------------------------|
| `trg_increase_stock` + `increase_stock()` | `PurchaseService.createPurchase()` |
| `trg_reduce_stock` + `reduce_stock()` | `BillingService.createBill()` |
| `trg_stock_audit` + `audit_stock_change()` | Both services write to `stock_audit` |
| `trg_update_purchase_total` | `PurchaseService` — inline calculation |
| `trg_update_bill_total` | `BillingService` — inline calculation |
| `low_stock_view` | `MedicineService.getLowStockMedicines()` |
| `expiry_alert_view` | `MedicineService.getExpiryAlerts()` |
| `sales_report` view | `ReportService.getSalesReport()` — MongoDB aggregation |

### MongoDB Transactions

MongoDB Atlas M0 (replica set) supports ACID multi-document transactions.

**Current implementation**: Application-level pre-validation ("Phase 1 stock check").  
The `BillingService` validates ALL items for sufficient stock **before** any writes. If any item fails, no stock is modified. This ensures consistency for single-threaded request processing without explicit `ClientSession` transaction overhead.

**For production scale**: Add explicit `ClientSession` transactions in `BillingService` and `PurchaseService` to handle concurrent billing of the same medicine stock.

### Performance Notes

- `GET /api/bills` returns the **50 most recent bills** by default.  
  Returning all 270+ bills triggers N+1 `@DBRef` customer resolution over Atlas.
- `GET /api/reports/sales` uses a **server-side MongoDB aggregation pipeline** (`$unwind → $group → $sort`).  
  This avoids loading all bills into Java memory and runs entirely on Atlas.

---

## ML Service: Python Demand Predictor

- **Port**: 5001 (use `ML_PORT=5001` — do NOT use `PORT` which conflicts with Spring Boot's 8080)
- **Framework**: Flask + PyMongo + Scikit-Learn
- **Data source**: `MONGODB_LIVE` when Atlas is reachable (545 bill-item records loaded in verified run)
- **Fallback**: `HISTORICAL_ENRICHED` (1,440 synthetic samples)

### Models Evaluated

1. Linear Regression
2. Random Forest Regressor (auto-selected if R² ≥ LinearRegression R²)

### Predictions

- 30-day demand per medicine
- Recommended reorder quantity: `max(0, predicted_demand + safety_buffer(15) - current_stock)`

### Key Fix (migration)
`medicine_id` in MongoDB is a string ObjectId. The feature was encoded using `pd.Categorical().codes` so scikit-learn receives integer features.

---

## Frontend: React + Vite

- **Port (dev)**: 3000
- **Build**: `npm run build` → `dist/` (198 kB JS, gzip 56 kB)
- **ID handling**: All entity IDs treated as **strings** (MongoDB ObjectId strings). No `parseInt()` on IDs.

---

## Environment Variables

| Service | Variable | Description |
|:--------|:---------|:----------- |
| Spring Boot | `MONGODB_URI` | Atlas connection string (required) |
| Spring Boot | `MONGODB_DATABASE` | Database name (default: `pharmacy_db`) |
| Spring Boot | `PORT` | HTTP port (default: 8080) |
| Spring Boot | `ML_SERVICE_URL` | ML API URL (default: `http://localhost:5001/api/predictions`) |
| ML Service | `MONGODB_URI` | Atlas URI (optional — falls back to HISTORICAL_ENRICHED) |
| ML Service | `ML_PORT` | Flask port (default: 5001; avoids conflict with Spring Boot PORT) |
| React | `VITE_API_URL` | Backend base URL (default: `http://localhost:8080`) |

---

## Security

- `.env` is in `.gitignore` and is **never committed to Git**
- `.env.example` contains placeholder URIs only (`<username>:<password>@<cluster>`)
- No credentials committed to any tracked file
- Demo seed credentials (`admin123`, `pharma123`) are **development-only** and documented as such
- ML service has **no psycopg2 or PostgreSQL dependency**

---

## Seed Script

```bash
cd database/seed
python3 seed_mongodb.py  # reads MONGODB_URI from .env
```

The seed is **idempotent** — safe to run multiple times. It generates 90 days of historical bills (544 items) for ML training.

Demo users (development only, not for production):
- `admin` / `admin123` (ADMIN role)
- `pharmacist` / `pharma123` (STAFF role)

---

## Verified Test Results

| Check | Result |
|:------|:-------|
| Atlas connection | ✅ PASS |
| Seed data | ✅ PASS |
| CRUD operations | ✅ PASS |
| Purchase stock increase | ✅ PASS |
| Billing stock decrease | ✅ PASS |
| Insufficient stock rejection | ✅ PASS (HTTP 400) |
| Expiry alerts | ✅ PASS (Amoxicillin 500mg, expiry 2026-10-28) |
| Stock audit | ✅ PASS (10 entries) |
| ML service | ✅ PASS (MONGODB_LIVE, 545 records, 9 predictions) |
| Maven tests | ✅ 12/12 PASS |
| Frontend build | ✅ PASS (198.32 kB) |
| Security scan | ✅ PASS |
