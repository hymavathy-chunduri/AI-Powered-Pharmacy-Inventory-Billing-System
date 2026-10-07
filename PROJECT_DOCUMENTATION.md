# Project Documentation — AI-Powered Pharmacy Inventory & Billing System

## Overview

This system is a full-stack pharmacy management solution built for B.Tech CSE Final Year, featuring:
- MongoDB Atlas cloud database (NoSQL)
- Spring Boot REST API backend
- React.js frontend (POS-style UI)
- Python Flask ML service for demand prediction

---

## Architecture

```
Browser (React UI)
        │
        │ HTTP REST API
        ▼
Spring Boot Backend (Java 17)
        │
        │ Spring Data MongoDB
        ▼
MongoDB Atlas (Cloud NoSQL)
        │
        │ PyMongo (aggregation pipeline)
        ▼
Python ML Module (Flask)
        │
        ▼
Demand Prediction + Reorder Recommendations
```

---

## Database: MongoDB Atlas

### Collections

| Collection   | Type             | Key Fields |
|--------------|------------------|------------|
| categories   | Independent      | category_name, description |
| suppliers    | Independent      | supplier_name, phone, email, address |
| medicines    | Independent + DBRef | medicine_name, category (→categories), price, stock_quantity, expiry_date |
| customers    | Independent      | customer_name, phone, email |
| purchases    | Parent + Embedded | supplier (→suppliers), purchase_date, total_amount, items[] |
| bills        | Parent + Embedded | customer (→customers), bill_date, total_amount, items[] |
| stock_audit  | Independent      | medicine (→medicines), old_stock, new_stock, changed_at |
| users        | Independent      | username, password, full_name, role |

### Embedded Documents

**Purchase.items[]**
```json
{
  "medicine_id": "ObjectId",
  "medicine_name": "Paracetamol 500mg",
  "quantity": 100,
  "unit_price": 5.50,
  "subtotal": 550.00
}
```

**Bill.items[]**
```json
{
  "medicine_id": "ObjectId",
  "medicine_name": "Paracetamol 500mg",
  "quantity": 2,
  "unit_price": 5.50,
  "subtotal": 11.00
}
```

---

## Business Logic (Service Layer)

All business logic that was previously implemented as PostgreSQL PL/pgSQL triggers is now implemented in the Spring Boot service layer.

### Purchase Flow
1. Validate supplier exists
2. For each item: validate medicine exists
3. Build Purchase with embedded PurchaseItem array
4. **Increase** `medicine.stock_quantity` by purchased quantity
5. Create `StockAudit` document (old_stock → new_stock)
6. Calculate `purchase.total_amount`
7. Save Purchase to MongoDB

### Billing Flow
1. Validate customer exists
2. **Pre-validate ALL items** for sufficient stock (fail-fast — no partial updates)
3. For each item: build BillItem with denormalized medicine name + current price
4. **Decrease** `medicine.stock_quantity` by billed quantity
5. Create `StockAudit` document (old_stock → new_stock)
6. Calculate `bill.total_amount`
7. Save Bill to MongoDB

### Insufficient Stock → HTTP 400
```json
{
  "errorCode": "INSUFFICIENT_STOCK",
  "message": "Insufficient stock for 'Paracetamol 500mg'. Available: 5, Requested: 10"
}
```

---

## REST API Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/categories | List categories |
| POST | /api/categories | Create category |
| PUT | /api/categories/{id} | Update category |
| DELETE | /api/categories/{id} | Delete category |
| GET | /api/suppliers | List suppliers |
| POST | /api/suppliers | Create supplier |
| PUT | /api/suppliers/{id} | Update supplier |
| DELETE | /api/suppliers/{id} | Delete supplier |
| GET | /api/medicines | List or search medicines |
| POST | /api/medicines | Create medicine |
| PUT | /api/medicines/{id} | Update medicine |
| DELETE | /api/medicines/{id} | Delete medicine |
| GET | /api/medicines/low-stock | Low-stock alert (stock ≤ 15) |
| GET | /api/medicines/expiry-alerts | Expiring within 30 days |
| GET | /api/customers | List customers |
| POST | /api/customers | Create customer |
| PUT | /api/customers/{id} | Update customer |
| DELETE | /api/customers/{id} | Delete customer |
| GET | /api/purchases | List purchases |
| POST | /api/purchases | Create purchase (increases stock) |
| GET | /api/bills | List bills |
| POST | /api/bills | Create bill (validates + decreases stock) |
| GET | /api/reports/inventory | Inventory report |
| GET | /api/reports/sales | Sales report |
| GET | /api/reports/audit | Stock audit log |
| GET | /api/predictions | ML demand predictions |

---

## ML Module

The Python Flask service (`pharmacy-ml/demand_predictor.py`):
- Reads bill data from MongoDB Atlas via PyMongo aggregation pipeline
- Trains Linear Regression + Random Forest Regressor models
- Selects the best model by R² score
- Returns 30-day demand predictions per medicine
- Calculates recommended reorder quantity
- Falls back to a 1,440-sample historical dataset when MongoDB is unavailable

---

## Environment Variables

| Variable | Service | Description |
|----------|---------|-------------|
| `MONGODB_URI` | Backend + ML | MongoDB Atlas connection string |
| `MONGODB_DATABASE` | Backend + ML | Database name (default: pharmacy_db) |
| `PORT` | Backend / ML | HTTP server port |
| `ML_SERVICE_URL` | Backend | URL of ML Flask service |
| `VITE_API_URL` | Frontend | Spring Boot API base URL |
