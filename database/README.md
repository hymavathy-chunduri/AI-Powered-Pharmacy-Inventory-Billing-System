# Database Seed — MongoDB Atlas

This directory contains seed scripts to populate the MongoDB Atlas `pharmacy_db` database with sample data.

## Prerequisites

- MongoDB Atlas cluster running and accessible
- `MONGODB_URI` environment variable set
- Python 3 and `pymongo` installed: `pip install pymongo`

## Running the Seed Script

```bash
cd database/seed
MONGODB_URI="mongodb+srv://<username>:<password>@<cluster>.mongodb.net/" python3 seed_mongodb.py
```

> **Note**: Replace the placeholder URI with your actual Atlas connection string.  
> Never commit the real connection string to Git.

## What Gets Seeded

- 5 medicine categories
- 3 suppliers
- 8 medicines (with category references)
- 4 customers

Purchases and bills are left empty — create them through the application UI.

## Collections Created

| Collection    | Documents |
|---------------|-----------|
| categories    | 5         |
| suppliers     | 3         |
| medicines     | 8         |
| customers     | 4         |
| purchases     | 0 (via UI)|
| bills         | 0 (via UI)|
| stock_audit   | 0 (via UI)|
