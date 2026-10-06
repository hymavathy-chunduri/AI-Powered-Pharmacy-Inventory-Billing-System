# 📚 College Final Review Project Report
## Pharmacy Inventory & Billing System with AI Demand Prediction

**Degree:** B.Tech Computer Science & Engineering  
**System Type:** Full-Stack Academic Project (DBMS + Spring Boot + React + Machine Learning)

---

## 1. Project Overview & Motivation
Pharmacies handle thousands of prescription items requiring strict inventory tracking, expiry monitoring, accurate customer billing, and stock reordering. Manual tracking leads to stock-outs of vital medicines or wastage from expired stock.

This project delivers a complete enterprise solution:
1. **Relational Database Engine (PostgreSQL)**: Handles ACID transactions, stock enforcement, and automatic audit logging via triggers.
2. **REST API Micro-service (Spring Boot)**: Encapsulates domain logic, validation, and REST API controllers.
3. **POS Dashboard (React + Vite)**: Provides an interactive UI for billing, stock management, and reporting.
4. **Demand Prediction Engine (Scikit-Learn Python)**: Uses historical sales data to predict 30-day demand and advise reorders.

---

## 2. Database Design & Integrity Constraints

### 2.1 Entity Relationship Diagram (Summary)
* **Categories (1:N)** -> Medicines
* **Suppliers (1:N)** -> Purchases (1:N) -> Purchase Items -> Medicines
* **Customers (1:N)** -> Bills (1:N) -> Bill Items -> Medicines
* **Medicines (1:N)** -> Stock Audit

### 2.2 Integrity Constraints Enforced
* `price > 0`
* `stock_quantity >= 0`
* `quantity > 0`
* Foreign Key Cascades & Foreign Key Checks

### 2.3 PL/pgSQL Triggers
* **`trg_reduce_stock`**: Fires `AFTER INSERT ON bill_items`. Executes `reduce_stock()`, verifies available stock, decrements `stock_quantity`, and records audit row.
* **`trg_increase_stock`**: Fires `AFTER INSERT ON purchase_items`. Executes `increase_stock()`, increments `stock_quantity`, and records audit row.

---

## 3. Backend & API Design
Built with **Spring Boot 3.2.4**, implementing a clean 4-tier architecture:
* **Controller Layer**: Exposes REST endpoints (`/api/medicines`, `/api/bills`, `/api/purchases`, `/api/reports`, `/api/predictions`).
* **Service Layer**: Business validation (`InsufficientStockException`), transaction management (`@Transactional`).
* **Repository Layer**: Spring Data JPA interfaces.
* **Database Layer**: PostgreSQL `pharmacy_db`.

---

## 4. Machine Learning & Predictive Analytics

### 4.1 Data Pipeline & Feature Selection
Historical sales data extracted from `bills` and `bill_items`.  
Features: `medicine_id`, `day_of_week`, `day_of_month`, `month`.

### 4.2 Model Performance Comparison
Both **Linear Regression** and **Random Forest Regressor** were evaluated on 80/20 train/test splits:
* **Random Forest Regressor** outperformed Linear Regression in capturing non-linear sales spikes and weekly purchasing cycles.

### 4.3 Reorder Decision Rule
$$\text{Reorder Quantity} = \max\left(0, \text{Predicted Demand}_{30\text{d}} + 15 - \text{Current Stock}\right)$$

---

## 5. System Limitations & Future Scope
* **Limitations**: Current historical dataset length in initial setup requires enrichment for multi-year seasonal forecasting.
* **Future Scope**: Integration with barcode scanners, multi-branch pharmacy support, and WhatsApp invoice delivery.
