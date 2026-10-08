# MongoDB Atlas Connection Information

> **SECURITY NOTE**: This file contains NO passwords, no real connection strings,
> and no secrets. It is safe to commit to Git.
> All real credentials live ONLY in the local `.env` file (gitignored).

---

## Atlas Project

| Field            | Value                                      |
|:-----------------|:-------------------------------------------|
| **Project name** | AI-Powered-Pharmacy-Inventory-Billing-System |
| **Cluster name** | pharmacy-cluster                           |
| **Tier**         | M0 Free (AWS ap-south-1)                   |
| **MongoDB ver**  | 8.0.34                                     |
| **Database**     | pharmacy_db                                |
| **DB Username**  | pharmacy_app                               |
| **Auth DB**      | admin                                      |

---

## Required Environment Variables

Set these in your local `.env` file (never commit `.env`):

```
MONGODB_URI=mongodb+srv://<USERNAME>:<PASSWORD>@<CLUSTER>.<HOST>.mongodb.net/
MONGODB_DATABASE=pharmacy_db
PORT=8080
ML_SERVICE_URL=http://localhost:5001/api/predictions
ML_PORT=5001
```

**Template (redacted):**

```
MONGODB_URI=mongodb+srv://pharmacy_app:<PASSWORD>@pharmacy-cluster.p11leil.mongodb.net/
MONGODB_DATABASE=pharmacy_db
```

> Replace `<PASSWORD>` with the `pharmacy_app` database user password.
> The real password is stored **only** in your local `.env` file.

---

## How to get the real connection string

1. Log in to [cloud.mongodb.com](https://cloud.mongodb.com)
2. Go to project **AI-Powered-Pharmacy-Inventory-Billing-System**
3. Click **pharmacy-cluster** → **Connect** → **Connect your application**
4. Select **Java** / **Spring Boot** or **Python** depending on the service
5. Copy the URI and replace `<password>` with your `pharmacy_app` password
6. Paste into your local `.env` as `MONGODB_URI=...`

---

## Network Access

Only allowlisted IPs can connect to the cluster.

To add your current IP:
```bash
~/bin/atlas accessLists create <YOUR_IP>/32 \
  --type ipAddress \
  --comment "Local dev" \
  --projectId <PROJECT_ID>
```

Or add via the Atlas console: **Network Access → + Add IP Address → Add Current IP Address**

> **Note**: M0 free-tier clusters auto-pause after inactivity.
> If you see a TLS `internal_error`, resume the cluster from the Atlas console
> (the Resume button appears on the cluster card when it is paused).

---

## Collections in pharmacy_db

| Collection    | Description                              |
|:--------------|:-----------------------------------------|
| categories    | Therapeutic categories (5)               |
| suppliers     | Medicine distributors (3)                |
| medicines     | Item master + stock + expiry (8)         |
| customers     | Customer directory (4+)                  |
| purchases     | Inbound supplier orders (3+)             |
| bills         | Customer invoices — 270+ historical      |
| stock_audit   | Automated stock change log               |
| users         | System users — dev demo credentials only |

---

## Where secrets are stored

| Environment  | Storage                        |
|:-------------|:-------------------------------|
| Local dev    | `.env` file (gitignored)       |
| CI/CD        | GitHub Secrets / env vars      |
| Production   | Cloud secrets manager / env    |

**The `.env` file is listed in `.gitignore` and must never be committed.**
