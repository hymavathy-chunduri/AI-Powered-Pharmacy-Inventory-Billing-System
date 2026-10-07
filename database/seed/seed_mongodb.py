"""
MongoDB Atlas Seed Script — Pharmacy Inventory & Billing System
================================================================
Populates the MongoDB pharmacy_db with sample data for development/demo.

Usage:
    MONGODB_URI="mongodb+srv://..." python3 seed_mongodb.py

WARNING: This will DROP existing categories, suppliers, medicines,
         and customers collections before inserting fresh data.
         Bills, purchases, and stock_audit are NOT dropped.
"""

import os
import sys
from datetime import date, timedelta

try:
    from pymongo import MongoClient
except ImportError:
    print("ERROR: pymongo not installed. Run: pip install pymongo")
    sys.exit(1)

MONGODB_URI = os.getenv("MONGODB_URI")
if not MONGODB_URI:
    print("ERROR: MONGODB_URI environment variable is not set.")
    print("Usage: MONGODB_URI='mongodb+srv://...' python3 seed_mongodb.py")
    sys.exit(1)

MONGODB_DATABASE = os.getenv("MONGODB_DATABASE", "pharmacy_db")

print(f"Connecting to MongoDB Atlas database: {MONGODB_DATABASE}")
client = MongoClient(MONGODB_URI)
db = client[MONGODB_DATABASE]

# --- Categories ---
print("Seeding categories...")
db["categories"].drop()
cat_result = db["categories"].insert_many([
    {"category_name": "Antibiotics",     "description": "Medicines that fight bacterial infections"},
    {"category_name": "Analgesics",      "description": "Pain relief medicines"},
    {"category_name": "Vitamins",        "description": "Nutritional supplements and vitamins"},
    {"category_name": "Antihistamines",  "description": "Allergy relief medicines"},
    {"category_name": "Antidiabetics",   "description": "Medicines for blood sugar management"},
])
cat_ids = cat_result.inserted_ids
print(f"  Inserted {len(cat_ids)} categories")

# --- Suppliers ---
print("Seeding suppliers...")
db["suppliers"].drop()
sup_result = db["suppliers"].insert_many([
    {"supplier_name": "MedPharma Distributors", "phone": "9876543210", "email": "contact@medpharma.com",   "address": "12 MedPharma Lane, Mumbai"},
    {"supplier_name": "HealthPlus Wholesale",   "phone": "9123456780", "email": "info@healthplus.com",     "address": "45 Health Street, Delhi"},
    {"supplier_name": "CureLine Pharma",        "phone": "9988776655", "email": "sales@curelinepharma.in", "address": "78 Pharma Park, Hyderabad"},
])
print(f"  Inserted {len(sup_result.inserted_ids)} suppliers")

# --- Medicines ---
print("Seeding medicines...")
db["medicines"].drop()
today = date.today()
medicines = [
    {
        "medicine_name": "Paracetamol 500mg",
        "category": {"$ref": "categories", "$id": cat_ids[1]},
        "price": 5.50,
        "stock_quantity": 200,
        "manufacture_date": str(today - timedelta(days=365)),
        "expiry_date":      str(today + timedelta(days=730)),
    },
    {
        "medicine_name": "Ibuprofen 400mg",
        "category": {"$ref": "categories", "$id": cat_ids[1]},
        "price": 8.00,
        "stock_quantity": 150,
        "manufacture_date": str(today - timedelta(days=300)),
        "expiry_date":      str(today + timedelta(days=700)),
    },
    {
        "medicine_name": "Amoxicillin 500mg",
        "category": {"$ref": "categories", "$id": cat_ids[0]},
        "price": 22.00,
        "stock_quantity": 10,   # Low stock intentionally for testing
        "manufacture_date": str(today - timedelta(days=180)),
        "expiry_date":      str(today + timedelta(days=20)),   # Expiry alert
    },
    {
        "medicine_name": "Azithromycin 500mg",
        "category": {"$ref": "categories", "$id": cat_ids[0]},
        "price": 35.00,
        "stock_quantity": 8,    # Low stock
        "manufacture_date": str(today - timedelta(days=90)),
        "expiry_date":      str(today + timedelta(days=400)),
    },
    {
        "medicine_name": "Vitamin C 500mg",
        "category": {"$ref": "categories", "$id": cat_ids[2]},
        "price": 3.00,
        "stock_quantity": 300,
        "manufacture_date": str(today - timedelta(days=60)),
        "expiry_date":      str(today + timedelta(days=900)),
    },
    {
        "medicine_name": "Cetirizine 10mg",
        "category": {"$ref": "categories", "$id": cat_ids[3]},
        "price": 6.00,
        "stock_quantity": 120,
        "manufacture_date": str(today - timedelta(days=120)),
        "expiry_date":      str(today + timedelta(days=600)),
    },
    {
        "medicine_name": "Metformin 500mg",
        "category": {"$ref": "categories", "$id": cat_ids[4]},
        "price": 12.00,
        "stock_quantity": 15,
        "manufacture_date": str(today - timedelta(days=200)),
        "expiry_date":      str(today + timedelta(days=500)),
    },
    {
        "medicine_name": "Insulin Glargine",
        "category": {"$ref": "categories", "$id": cat_ids[4]},
        "price": 250.00,
        "stock_quantity": 5,    # Low stock
        "manufacture_date": str(today - timedelta(days=30)),
        "expiry_date":      str(today + timedelta(days=180)),
    },
]
db["medicines"].insert_many(medicines)
print(f"  Inserted {len(medicines)} medicines")

# --- Customers ---
print("Seeding customers...")
db["customers"].drop()
db["customers"].insert_many([
    {"customer_name": "Priya Sharma",   "phone": "9111222333", "email": "priya@example.com"},
    {"customer_name": "Ravi Kumar",     "phone": "9222333444", "email": "ravi@example.com"},
    {"customer_name": "Lakshmi Devi",   "phone": "9333444555", "email": "lakshmi@example.com"},
    {"customer_name": "Arjun Reddy",    "phone": "9444555666", "email": "arjun@example.com"},
])
print("  Inserted 4 customers")

client.close()
print("\n✅ Seed complete! MongoDB Atlas pharmacy_db is ready.")
print("   Start the Spring Boot backend and React frontend to use the application.")
