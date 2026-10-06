# 💊 Pharmacy Inventory & Billing System

A complete **B.Tech CSE Final-Year Project System** featuring an integrated PostgreSQL relational database, Spring Boot REST backend, modern React POS frontend, and Scikit-Learn Python ML Demand Prediction Module.

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
                                 Spring Data JPA
                                       │
                                       ▼
                                ┌─────────────┐
                                │ PostgreSQL  │
                                │ pharmacy_db │
                                └──────┬──────┘
                                       │
                                Historical Sales
                                       │
                                       ▼
                                ┌─────────────┐
                                │ Python ML   │
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
| **Database** | PostgreSQL 17, PL/pgSQL, Triggers, Views, Functions |
| **Backend** | Java 17, Spring Boot 3.2.4, Spring Data JPA, Jakarta Validation |
| **Frontend** | React 18, Vite 5, Lucide Icons, Modern Vanilla CSS |
| **Machine Learning** | Python 3, Scikit-Learn, Pandas, NumPy, Flask |
| **Build Tools** | Maven 3.9, npm |

---

## 🗄️ Database Schema & Features (`pharmacy_db`)

The PostgreSQL database contains **10 core relational tables**:
1. `users` — System authentication & role management
2. `categories` — Medicine therapeutic categories
3. `suppliers` — Medicine distributors & vendor details
4. `medicines` — Item master, unit prices, stock level, expiry date
5. `customers` — Customer directory & contact records
6. `purchases` — Inbound supplier purchase orders
7. `purchase_items` — Itemized purchase order breakdown
8. `bills` — Customer sales invoices & payment modes
9. `bill_items` — Itemized sale invoice breakdown
10. `stock_audit` — Automated audit log populated by PostgreSQL triggers

### Database Automation (PL/pgSQL Triggers & Views)
* `trg_reduce_stock` -> Executes `reduce_stock()` after billing to decrease stock and record audit log.
* `trg_increase_stock` -> Executes `increase_stock()` after purchase to increase stock and record audit log.
* `inventory_view`, `sales_report`, `low_stock_view`, `expiry_alert_view` -> Reporting views.

---

## 🚀 Getting Started & Running the System

### 1. Prerequisites
Ensure PostgreSQL 17 is running on port `5432` with database `pharmacy_db`.

### 2. Start Python ML Module
```bash
cd pharmacy-ml
python3 demand_predictor.py
```
*Runs on `http://localhost:5001`*

### 3. Start Spring Boot Backend
```bash
cd pharmacy-backend
mvn spring-boot:run
```
*Runs on `http://localhost:8080`*

### 4. Start React Frontend
```bash
cd pharmacy-frontend
npm run dev
```
*Runs on `http://localhost:3000`*

---

## 📡 Core REST API Endpoints

### Medicines API
* `GET /api/medicines` — List all medicines or search (`?search=Paracetamol`)
* `GET /api/medicines/{id}` — Fetch medicine details
* `POST /api/medicines` — Create new medicine
* `PUT /api/medicines/{id}` — Update medicine details
* `DELETE /api/medicines/{id}` — Delete medicine
* `GET /api/medicines/low-stock` — Fetch low-stock alert list
* `GET /api/medicines/expiry-alerts` — Fetch expiring medicines list

### Billing & POS API
* `POST /api/bills` — Process customer bill (pre-validates stock, triggers automated PostgreSQL stock reduction & audit log)
* `GET /api/bills` — Fetch all customer bills

### Purchases API
* `POST /api/purchases` — Record supplier shipment (triggers automated PostgreSQL stock increase)
* `GET /api/purchases` — Fetch all purchases

### AI Demand Prediction API
* `GET /api/predictions` — Fetch ML model metrics (Linear Regression vs Random Forest), 30-day demand predictions & reorder recommendations.

---

## 🤖 AI / Machine Learning Methodology

### Feature Engineering
Model input features: `medicine_id`, `day_of_week`, `day_of_month`, `month`.

### Evaluated Models
1. **Linear Regression**
2. **Random Forest Regressor** (Champion Model)

### Model Evaluation Metrics
* **MAE** (Mean Absolute Error)
* **RMSE** (Root Mean Squared Error)
* **R² Score** (Coefficient of Determination)

### Reorder Recommendation Formula
$$\text{Recommended Reorder Qty} = \max\left(0, \text{Predicted 30-Day Demand} + \text{Safety Buffer (15)} - \text{Current Stock}\right)$$

---

## 🔬 Testing & Verification

1. **Backend Tests**: Run `mvn test` inside `pharmacy-backend`.
2. **Stock Validation**: Attempts to bill > available stock return HTTP 400 `INSUFFICIENT_STOCK`.
3. **Database Audit**: All stock updates automatically generate rows in `stock_audit`.

---

## 🌐 Production Deployment Configuration

### Environment Variables Matrix

| Service | Environment Variable | Default Value | Description |
| :--- | :--- | :--- | :--- |
| **PostgreSQL Database** | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/pharmacy_db` | Database JDBC URL |
| | `SPRING_DATASOURCE_USERNAME` | `postgres` | Database username |
| | `SPRING_DATASOURCE_PASSWORD` / `PGPASSWORD` | *(None)* | Database password |
| **Spring Boot Backend** | `PORT` | `8080` | Backend HTTP server port |
| | `ML_SERVICE_URL` | `http://localhost:5001/api/predictions` | Microservice URL to Python ML service |
| **Python ML Module** | `PORT` | `5001` | ML Flask service port |
| | `PGHOST` / `PGPORT` / `PGDATABASE` | `localhost` / `5432` / `pharmacy_db` | PostgreSQL connection parameters |
| **React Frontend** | `VITE_API_URL` | `http://localhost:8080` | Production Spring Boot API base URL |

### Local Development Commands
```bash
# 1. Start Python ML Module
cd pharmacy-ml && python3 demand_predictor.py

# 2. Start Spring Boot Backend
cd pharmacy-backend && PGPASSWORD=your_db_password mvn spring-boot:run

# 3. Start React Frontend
cd pharmacy-frontend && npm run dev
```
