"""
MongoDB Atlas Seed Script — Pharmacy Inventory & Billing System
================================================================
Idempotent:
  - categories, suppliers, medicines, customers, users: always dropped + re-inserted
  - purchases, bills, stock_audit: only inserted if collection is empty / too few docs

Usage:
    MONGODB_URI="mongodb+srv://..." python3 seed_mongodb.py
"""

import os, sys, datetime, random
from datetime import date, timedelta

try:
    from pymongo import MongoClient
except ImportError:
    print("ERROR: pymongo not installed. Run: pip install pymongo"); sys.exit(1)

MONGODB_URI = os.getenv("MONGODB_URI")
if not MONGODB_URI:
    print("ERROR: MONGODB_URI not set."); sys.exit(1)

MONGODB_DATABASE = os.getenv("MONGODB_DATABASE", "pharmacy_db")
print(f"Connecting to MongoDB Atlas database: {MONGODB_DATABASE}")
client = MongoClient(MONGODB_URI)
db = client[MONGODB_DATABASE]
today = date.today()
random.seed(42)

# ── Categories
print("Seeding categories...")
db["categories"].drop()
cats = db["categories"].insert_many([
    {"category_name": "Antibiotics",    "description": "Medicines that fight bacterial infections"},
    {"category_name": "Analgesics",     "description": "Pain relief medicines"},
    {"category_name": "Vitamins",       "description": "Nutritional supplements and vitamins"},
    {"category_name": "Antihistamines", "description": "Allergy relief medicines"},
    {"category_name": "Antidiabetics",  "description": "Medicines for blood sugar management"},
])
cat_ids = cats.inserted_ids
print(f"  Inserted {len(cat_ids)} categories")

# ── Suppliers
print("Seeding suppliers...")
db["suppliers"].drop()
sups = db["suppliers"].insert_many([
    {"supplier_name": "MedPharma Distributors", "phone": "9876543210", "email": "contact@medpharma.com",   "address": "12 MedPharma Lane, Mumbai"},
    {"supplier_name": "HealthPlus Wholesale",   "phone": "9123456780", "email": "info@healthplus.com",     "address": "45 Health Street, Delhi"},
    {"supplier_name": "CureLine Pharma",        "phone": "9988776655", "email": "sales@curelinepharma.in", "address": "78 Pharma Park, Hyderabad"},
])
sup_ids = sups.inserted_ids
print(f"  Inserted {len(sup_ids)} suppliers")

# ── Medicines
print("Seeding medicines...")
db["medicines"].drop()
meds_data = [
    {"medicine_name": "Paracetamol 500mg",  "category": {"$ref":"categories","$id":cat_ids[1]}, "price":5.50,   "stock_quantity":200, "manufacture_date":datetime.datetime(*(today-timedelta(days=365)).timetuple()[:3]), "expiry_date":datetime.datetime(*(today+timedelta(days=730)).timetuple()[:3])},
    {"medicine_name": "Ibuprofen 400mg",    "category": {"$ref":"categories","$id":cat_ids[1]}, "price":8.00,   "stock_quantity":150, "manufacture_date":datetime.datetime(*(today-timedelta(days=300)).timetuple()[:3]), "expiry_date":datetime.datetime(*(today+timedelta(days=700)).timetuple()[:3])},
    {"medicine_name": "Amoxicillin 500mg",  "category": {"$ref":"categories","$id":cat_ids[0]}, "price":22.00,  "stock_quantity":10,  "manufacture_date":datetime.datetime(*(today-timedelta(days=180)).timetuple()[:3]), "expiry_date":datetime.datetime(*(today+timedelta(days=20)).timetuple()[:3])},
    {"medicine_name": "Azithromycin 500mg", "category": {"$ref":"categories","$id":cat_ids[0]}, "price":35.00,  "stock_quantity":8,   "manufacture_date":datetime.datetime(*(today-timedelta(days=90)).timetuple()[:3]),  "expiry_date":datetime.datetime(*(today+timedelta(days=400)).timetuple()[:3])},
    {"medicine_name": "Vitamin C 500mg",    "category": {"$ref":"categories","$id":cat_ids[2]}, "price":3.00,   "stock_quantity":300, "manufacture_date":datetime.datetime(*(today-timedelta(days=60)).timetuple()[:3]),  "expiry_date":datetime.datetime(*(today+timedelta(days=900)).timetuple()[:3])},
    {"medicine_name": "Cetirizine 10mg",    "category": {"$ref":"categories","$id":cat_ids[3]}, "price":6.00,   "stock_quantity":120, "manufacture_date":datetime.datetime(*(today-timedelta(days=120)).timetuple()[:3]), "expiry_date":datetime.datetime(*(today+timedelta(days=600)).timetuple()[:3])},
    {"medicine_name": "Metformin 500mg",    "category": {"$ref":"categories","$id":cat_ids[4]}, "price":12.00,  "stock_quantity":15,  "manufacture_date":datetime.datetime(*(today-timedelta(days=200)).timetuple()[:3]), "expiry_date":datetime.datetime(*(today+timedelta(days=500)).timetuple()[:3])},
    {"medicine_name": "Insulin Glargine",   "category": {"$ref":"categories","$id":cat_ids[4]}, "price":250.00, "stock_quantity":5,   "manufacture_date":datetime.datetime(*(today-timedelta(days=30)).timetuple()[:3]),  "expiry_date":datetime.datetime(*(today+timedelta(days=180)).timetuple()[:3])},
]
med_r = db["medicines"].insert_many(meds_data)
med_ids = med_r.inserted_ids
print(f"  Inserted {len(med_ids)} medicines")

# ── Customers
print("Seeding customers...")
db["customers"].drop()
cust_r = db["customers"].insert_many([
    {"customer_name":"Priya Sharma",  "phone":"9111222333","email":"priya@example.com"},
    {"customer_name":"Ravi Kumar",    "phone":"9222333444","email":"ravi@example.com"},
    {"customer_name":"Lakshmi Devi",  "phone":"9333444555","email":"lakshmi@example.com"},
    {"customer_name":"Arjun Reddy",   "phone":"9444555666","email":"arjun@example.com"},
])
cust_ids = cust_r.inserted_ids
print(f"  Inserted {len(cust_ids)} customers")

# ── Users
print("Seeding users...")
db["users"].drop()
db["users"].insert_many([
    {"username":"admin",      "password":"admin123",  "full_name":"Admin User",      "role":"ADMIN","created_at":datetime.datetime.utcnow()},
    {"username":"pharmacist", "password":"pharma123", "full_name":"Pharmacy Staff",  "role":"STAFF","created_at":datetime.datetime.utcnow()},
])
print("  Inserted 2 users")

# ── Purchases (idempotent)
if db["purchases"].count_documents({}) > 0:
    print(f"  Purchases: {db['purchases'].count_documents({})} already exist — skipping")
else:
    print("Seeding purchases...")
    db["purchases"].insert_many([
        {"supplier":{"$ref":"suppliers","$id":sup_ids[0]},"purchase_date":datetime.datetime(*(today-timedelta(days=60)).timetuple()[:3]),"total_amount":1090.0,
         "items":[{"medicine_id":str(med_ids[0]),"medicine_name":"Paracetamol 500mg","quantity":100,"unit_price":5.50,"subtotal":550.0},
                  {"medicine_id":str(med_ids[4]),"medicine_name":"Vitamin C 500mg","quantity":100,"unit_price":3.00,"subtotal":300.0},
                  {"medicine_id":str(med_ids[5]),"medicine_name":"Cetirizine 10mg","quantity":40,"unit_price":6.00,"subtotal":240.0}]},
        {"supplier":{"$ref":"suppliers","$id":sup_ids[1]},"purchase_date":datetime.datetime(*(today-timedelta(days=30)).timetuple()[:3]),"total_amount":1740.0,
         "items":[{"medicine_id":str(med_ids[1]),"medicine_name":"Ibuprofen 400mg","quantity":80,"unit_price":8.00,"subtotal":640.0},
                  {"medicine_id":str(med_ids[2]),"medicine_name":"Amoxicillin 500mg","quantity":30,"unit_price":22.00,"subtotal":660.0},
                  {"medicine_id":str(med_ids[3]),"medicine_name":"Azithromycin 500mg","quantity":12,"unit_price":35.00,"subtotal":420.0}]},
        {"supplier":{"$ref":"suppliers","$id":sup_ids[2]},"purchase_date":datetime.datetime(*(today-timedelta(days=15)).timetuple()[:3]),"total_amount":4548.0,
         "items":[{"medicine_id":str(med_ids[6]),"medicine_name":"Metformin 500mg","quantity":50,"unit_price":12.00,"subtotal":600.0},
                  {"medicine_id":str(med_ids[7]),"medicine_name":"Insulin Glargine","quantity":15,"unit_price":250.00,"subtotal":3750.0},
                  {"medicine_id":str(med_ids[0]),"medicine_name":"Paracetamol 500mg","quantity":36,"unit_price":5.50,"subtotal":198.0}]},
    ])
    print("  Inserted 3 purchases")

# ── Bills — 90 days historical (for ML LIVE_MONGODB) (idempotent)
existing_bills = db["bills"].count_documents({})
if existing_bills >= 30:
    print(f"  Bills: {existing_bills} already exist (>=30) — ML will use LIVE_MONGODB")
else:
    print("Seeding 90 days of historical bills (ML training data)...")
    db["bills"].drop()
    bill_meds = [
        (str(med_ids[0]),"Paracetamol 500mg",5.50),
        (str(med_ids[1]),"Ibuprofen 400mg",8.00),
        (str(med_ids[2]),"Amoxicillin 500mg",22.00),
        (str(med_ids[3]),"Azithromycin 500mg",35.00),
        (str(med_ids[4]),"Vitamin C 500mg",3.00),
        (str(med_ids[5]),"Cetirizine 10mg",6.00),
        (str(med_ids[6]),"Metformin 500mg",12.00),
        (str(med_ids[7]),"Insulin Glargine",250.00),
    ]
    cust_refs = [{"$ref":"customers","$id":cid} for cid in cust_ids]
    bills_batch = []
    for day_offset in range(90,0,-1):
        bill_dt = datetime.datetime.utcnow() - datetime.timedelta(days=day_offset)
        for _ in range(random.randint(2,4)):
            selected = random.sample(bill_meds, k=random.randint(1,3))
            items, total = [], 0.0
            for mid,mname,mprice in selected:
                qty = random.randint(1,10)
                sub = round(qty*mprice,2)
                total += sub
                items.append({"medicine_id":mid,"medicine_name":mname,"quantity":qty,"unit_price":mprice,"subtotal":sub})
            bills_batch.append({"customer":random.choice(cust_refs),"bill_date":bill_dt,"total_amount":round(total,2),"items":items})
    db["bills"].insert_many(bills_batch)
    nitems = sum(len(b["items"]) for b in bills_batch)
    print(f"  Inserted {len(bills_batch)} bills ({nitems} items) — ML will use LIVE_MONGODB")

# ── Stock Audit (idempotent)
if db["stock_audit"].count_documents({}) == 0:
    print("Seeding initial stock_audit...")
    db["stock_audit"].insert_many([
        {"medicine":{"$ref":"medicines","$id":mid},"old_stock":0,"new_stock":meds_data[i]["stock_quantity"],
         "changed_at":datetime.datetime.utcnow()-datetime.timedelta(days=90)}
        for i,mid in enumerate(med_ids)
    ])
    print(f"  Inserted {len(med_ids)} stock audit records")
else:
    print(f"  Stock audit: {db['stock_audit'].count_documents({})} records exist — skipping")

client.close()
print("\n Seed complete! MongoDB Atlas pharmacy_db is ready.")
print("   Start the Spring Boot backend and React frontend to use the application.")
