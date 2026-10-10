import os
import pymongo
import random
import datetime
from dotenv import load_dotenv
import dns.resolver
try:
    import certifi
    ca_file = certifi.where()
except ImportError:
    ca_file = None

dns.resolver.default_resolver = dns.resolver.Resolver(configure=False)
dns.resolver.default_resolver.nameservers = ['8.8.8.8']

load_dotenv()
mongo_uri = os.environ.get("MONGODB_URI")
if not mongo_uri:
    raise ValueError("MONGODB_URI is not set in .env")

client_kwargs = {"serverSelectionTimeoutMS": 10000}
if ca_file:
    client_kwargs["tlsCAFile"] = ca_file

client = pymongo.MongoClient(mongo_uri, **client_kwargs)
db = client["pharmacy_db"]

# ──────────────────────────────────────────────────────────────────────────────
# MIGRATION: convert @DBRef format to plain ObjectId (required for
# @DocumentReference batch loading in Spring Data MongoDB 3.x).
# Collections that stored DBRef objects must be dropped and re-seeded so that
# Spring can read them without N+1 individual fetches.
# ──────────────────────────────────────────────────────────────────────────────
def _has_dbref(collection, field):
    """Return True if any document in the collection has a DBRef in `field`."""
    sample = collection.find_one({field: {"$type": "object"}})
    if sample and isinstance(sample.get(field), dict) and "$ref" in sample[field]:
        return True
    try:
        from bson.dbref import DBRef as _DBRef
        sample2 = collection.find_one({field: {"$type": "dbPointer"}})
        if sample2:
            return True
    except Exception:
        pass
    return False

def _is_dbref_bson(val):
    try:
        from bson.dbref import DBRef as _DBRef
        return isinstance(val, _DBRef)
    except Exception:
        return False

def _resolve_ref(val):
    """Extract the ObjectId from a DBRef or plain dict {'$ref':..,'$id':..}."""
    if _is_dbref_bson(val):
        return val.id
    if isinstance(val, dict) and "$id" in val:
        return val["$id"]
    return val  # already a plain id

def _migrate_collection(coll, field):
    """Replace DBRef values in `field` with plain ObjectIds."""
    updated = 0
    for doc in coll.find():
        val = doc.get(field)
        if val is None:
            continue
        if _is_dbref_bson(val) or (isinstance(val, dict) and "$ref" in val):
            coll.update_one({"_id": doc["_id"]}, {"$set": {field: _resolve_ref(val)}})
            updated += 1
    if updated:
        print(f"  Migrated {updated} docs in {coll.name}.{field}")

print("Checking/migrating DBRef → plain ObjectId …")
_migrate_collection(db.medicines, "category")
_migrate_collection(db.purchases, "supplier")
_migrate_collection(db.bills, "customer")
_migrate_collection(db.stock_audit, "medicine")
print("Migration done.")


categories_data = [
    "Analgesics", "Antibiotics", "Vitamins", "Antihistamines", "Antidiabetics",
    "Cardiovascular", "Dermatological", "Gastrointestinal", "Respiratory", "Neurological",
    "Psychiatric", "Ophthalmic"
]

suppliers_data = [
    "PharmaCorp", "HealthSupplies Inc", "GlobalMeds", "MediCare Solutions", "LifeCare Distributors",
    "WellBeing Pharma", "Apex Medical", "NovaHealth", "PrimeTherapeutics", "BioPharm Direct",
    "CureAll Distributors", "Sunrise Pharmaceuticals", "TrueHealth Suppliers", "Vitality Meds", "Optima Health"
]

medicines_data = [
    ("Paracetamol 500mg", "Analgesics", 5.50), ("Ibuprofen 400mg", "Analgesics", 8.00),
    ("Amoxicillin 500mg", "Antibiotics", 22.00), ("Azithromycin 500mg", "Antibiotics", 35.00),
    ("Vitamin C 500mg", "Vitamins", 3.00), ("Cetirizine 10mg", "Antihistamines", 6.00),
    ("Metformin 500mg", "Antidiabetics", 12.00), ("Insulin Glargine", "Antidiabetics", 250.00),
    ("Aspirin 81mg", "Analgesics", 4.00), ("Lisinopril 10mg", "Cardiovascular", 15.00),
    ("Amlodipine 5mg", "Cardiovascular", 18.00), ("Simvastatin 20mg", "Cardiovascular", 14.00),
    ("Omeprazole 20mg", "Gastrointestinal", 12.50), ("Pantoprazole 40mg", "Gastrointestinal", 9.50),
    ("Levothyroxine 50mcg", "Neurological", 20.00), ("Sertraline 50mg", "Psychiatric", 25.00),
    ("Fluoxetine 20mg", "Psychiatric", 22.00), ("Albuterol Inhaler", "Respiratory", 45.00),
    ("Fluticasone Nasal Spray", "Respiratory", 35.00), ("Hydrocortisone Cream 1%", "Dermatological", 8.00),
    ("Clotrimazole Cream 1%", "Dermatological", 9.00), ("Timolol Eye Drops 0.5%", "Ophthalmic", 28.00),
    ("Latanoprost Eye Drops", "Ophthalmic", 55.00), ("Vitamin D3 1000 IU", "Vitamins", 5.00),
    ("B-Complex Tablets", "Vitamins", 7.50), ("Loratadine 10mg", "Antihistamines", 6.50),
    ("Diphenhydramine 25mg", "Antihistamines", 5.00), ("Glipizide 5mg", "Antidiabetics", 11.00),
    ("Losartan 50mg", "Cardiovascular", 19.00), ("Atorvastatin 20mg", "Cardiovascular", 18.00),
    ("Rosuvastatin 10mg", "Cardiovascular", 25.00), ("Esomeprazole 40mg", "Gastrointestinal", 15.00),
    ("Ranitidine 150mg", "Gastrointestinal", 8.50), ("Montelukast 10mg", "Respiratory", 30.00),
    ("Budesonide Inhaler", "Respiratory", 50.00), ("Gabapentin 300mg", "Neurological", 18.00),
    ("Pregabalin 75mg", "Neurological", 35.00), ("Citalopram 20mg", "Psychiatric", 20.00),
    ("Escitalopram 10mg", "Psychiatric", 24.00), ("Tretinoin Cream 0.025%", "Dermatological", 40.00),
    ("Mupirocin Ointment 2%", "Dermatological", 15.00), ("Artificial Tears", "Ophthalmic", 12.00),
    ("Moxifloxacin Eye Drops", "Ophthalmic", 35.00), ("Iron Supplements 65mg", "Vitamins", 6.00),
    ("Calcium + Vitamin D", "Vitamins", 8.00), ("Fexofenadine 180mg", "Antihistamines", 14.00),
    ("Promethazine 25mg", "Antihistamines", 10.00), ("Pioglitazone 15mg", "Antidiabetics", 28.00),
    ("Sitagliptin 100mg", "Antidiabetics", 45.00), ("Metoprolol 50mg", "Cardiovascular", 12.00),
    ("Carvedilol 6.25mg", "Cardiovascular", 16.00), ("Ondansetron 4mg", "Gastrointestinal", 18.00),
    ("Metoclopramide 10mg", "Gastrointestinal", 10.00), ("Ipratropium Inhaler", "Respiratory", 38.00),
    ("Salmeterol Inhaler", "Respiratory", 60.00), ("Topiramate 50mg", "Neurological", 22.00),
    ("Duloxetine 30mg", "Psychiatric", 30.00), ("Benzoyl Peroxide 5%", "Dermatological", 11.00),
    ("Ketoconazole Shampoo 2%", "Dermatological", 20.00), ("Olopatadine Eye Drops", "Ophthalmic", 42.00)
]

customers_data = [
    ("Alice Smith", "1234567890", "alice@example.com"), ("Bob Johnson", "2345678901", "bob@example.com"),
    ("Charlie Brown", "3456789012", "charlie@example.com"), ("Diana Prince", "4567890123", "diana@example.com"),
    ("Evan Wright", "5678901234", "evan@example.com"), ("Fiona Gallagher", "6789012345", "fiona@example.com"),
    ("George Miller", "7890123456", "george@example.com"), ("Hannah Abbott", "8901234567", "hannah@example.com"),
    ("Ian Malcolm", "9012345678", "ian@example.com"), ("Julia Roberts", "0123456789", "julia@example.com"),
    ("Kevin Hart", "1122334455", "kevin@example.com"), ("Laura Dern", "2233445566", "laura@example.com"),
    ("Michael Scott", "3344556677", "michael@example.com"), ("Nina Dobrev", "4455667788", "nina@example.com"),
    ("Oscar Isaac", "5566778899", "oscar@example.com"), ("Pam Beesly", "6677889900", "pam@example.com"),
    ("Quinn Fabray", "7788990011", "quinn@example.com"), ("Rachel Green", "8899001122", "rachel@example.com"),
    ("Sam Winchester", "9900112233", "sam@example.com"), ("Tina Cohen", "0011223344", "tina@example.com"),
    ("Ulysses Klaue", "1231231234", "ulysses@example.com"), ("Victoria Justice", "2342342345", "victoria@example.com"),
    ("Will Smith", "3453453456", "will@example.com"), ("Xena Warrior", "4564564567", "xena@example.com"),
    ("Yusuf Islam", "5675675678", "yusuf@example.com"), ("Zoe Saldana", "6786786789", "zoe@example.com"),
    ("Aaron Paul", "7897897890", "aaron@example.com"), ("Bella Swan", "8908908901", "bella@example.com"),
    ("Caleb Rivers", "9019019012", "caleb@example.com"), ("Daisy Johnson", "0120120123", "daisy@example.com"),
    ("Ethan Hunt", "1351351357", "ethan@example.com"), ("Frank Castle", "2462462468", "frank@example.com"),
    ("Gwen Stacy", "3573573579", "gwen@example.com"), ("Harry Potter", "4684684680", "harry@example.com"),
    ("Iris West", "5795795791", "iris@example.com"), ("Jack Sparrow", "6806806802", "jack@example.com"),
    ("Kara Danvers", "7917917913", "kara@example.com"), ("Luke Skywalker", "8028028024", "luke@example.com"),
    ("Mia Thermopolis", "9139139135", "mia@example.com"), ("Noah Calhoun", "0240240246", "noah@example.com")
]

today = datetime.datetime.now(datetime.timezone.utc)

# 1. Categories
existing_cats = {c["category_name"]: c["_id"] for c in db.categories.find()}
new_cats = [
    {"category_name": c, "description": f"{c} medicines"}
    for c in categories_data if c not in existing_cats
]
if new_cats: db.categories.insert_many(new_cats)
cat_map = {c["category_name"]: c["_id"] for c in db.categories.find()}

# 2. Suppliers
existing_sups = {s["supplier_name"]: s["_id"] for s in db.suppliers.find()}
new_sups = [
    {"supplier_name": s, "contact_info": f"contact@{s.lower().replace(' ', '')}.com"}
    for s in suppliers_data if s not in existing_sups
]
if new_sups: db.suppliers.insert_many(new_sups)
sup_map = {s["supplier_name"]: s["_id"] for s in db.suppliers.find()}

# 3. Medicines
existing_meds = {m["medicine_name"]: m["_id"] for m in db.medicines.find()}
new_meds = []
stock_audit_batch = []
for m_name, cat, price in medicines_data:
    if m_name not in existing_meds:
        cat_id = cat_map[cat]
        doc = {
            "medicine_name": m_name,
            "category": cat_id,
            "price": price,
            "stock_quantity": random.randint(10, 300),
            "manufacture_date": datetime.datetime(*(today-datetime.timedelta(days=random.randint(30, 365))).timetuple()[:3]),
            "expiry_date": datetime.datetime(*(today+datetime.timedelta(days=random.randint(180, 1000))).timetuple()[:3]),
        }
        new_meds.append(doc)
if new_meds:
    res = db.medicines.insert_many(new_meds)
    for i, m_id in enumerate(res.inserted_ids):
        stock_audit_batch.append({
            "medicine": m_id,
            "old_stock": 0,
            "new_stock": new_meds[i]["stock_quantity"],
            "changed_at": today - datetime.timedelta(days=random.randint(1, 30))
        })
    if stock_audit_batch: db.stock_audit.insert_many(stock_audit_batch)
med_map = {m["medicine_name"]: m["_id"] for m in db.medicines.find()}

# 4. Customers
existing_custs = {c["phone"]: c["_id"] for c in db.customers.find()}
new_custs = [
    {"customer_name": c_name, "phone": phone, "email": email}
    for c_name, phone, email in customers_data if phone not in existing_custs
]
if new_custs: db.customers.insert_many(new_custs)
cust_map = {c["phone"]: c["_id"] for c in db.customers.find()}

# 5. Users
if db.users.count_documents({}) < 2:
    db.users.insert_many([
        {"username": "admin", "password_hash": "admin123", "role": "ADMIN"},
        {"username": "pharmacist", "password_hash": "pharm123", "role": "PHARMACIST"}
    ])

# 6. Purchases (need 35+)
existing_purchases = db.purchases.count_documents({})
sup_list = list(sup_map.values())
med_items = list(med_map.items()) # (name, _id)
if existing_purchases < 35:
    purchases_batch = []
    for _ in range(35 - existing_purchases):
        selected_meds = random.sample(med_items, k=random.randint(2, 5))
        items = []
        total = 0.0
        for m_name, m_id in selected_meds:
            qty = random.randint(20, 100)
            uprice = [x[2] for x in medicines_data if x[0] == m_name][0] * 0.7
            sub = round(qty * uprice, 2)
            total += sub
            items.append({"medicine_id": str(m_id), "medicine_name": m_name, "quantity": qty, "unit_price": round(uprice, 2), "subtotal": sub})
        purchases_batch.append({
            "supplier": random.choice(sup_list),
            "purchase_date": today - datetime.timedelta(days=random.randint(1, 180)),
            "total_amount": round(total, 2),
            "items": items
        })
    db.purchases.insert_many(purchases_batch)

# 7. Bills (need 150+)
existing_bills = db.bills.count_documents({})
cust_id_list = list(cust_map.values())
if existing_bills < 150:
    bills_batch = []
    for _ in range(150 - existing_bills):
        selected_meds = random.sample(med_items, k=random.randint(1, 4))
        items = []
        total = 0.0
        for m_name, m_id in selected_meds:
            qty = random.randint(1, 5)
            uprice = [x[2] for x in medicines_data if x[0] == m_name][0]
            sub = round(qty * uprice, 2)
            total += sub
            items.append({"medicine_id": str(m_id), "medicine_name": m_name, "quantity": qty, "unit_price": uprice, "subtotal": sub})
        bills_batch.append({
            "customer": random.choice(cust_id_list),
            "bill_date": today - datetime.timedelta(days=random.randint(1, 180)),
            "total_amount": round(total, 2),
            "items": items,
            "payment_mode": random.choice(["CASH", "CARD", "UPI"])
        })
    db.bills.insert_many(bills_batch)

print("Seed data complete.")
print(f"Categories: {db.categories.count_documents({})}")
print(f"Suppliers: {db.suppliers.count_documents({})}")
print(f"Medicines: {db.medicines.count_documents({})}")
print(f"Customers: {db.customers.count_documents({})}")
print(f"Purchases: {db.purchases.count_documents({})}")
print(f"Bills: {db.bills.count_documents({})}")
