import requests, json, time, sys, os

# Safely load environment variables from .env if present
env_path = os.path.join(os.path.dirname(__file__), ".env")
if os.path.exists(env_path):
    with open(env_path) as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                os.environ.setdefault(k.strip(), v.strip().strip("'").strip('"'))

REG_CODE = os.getenv("EMPLOYEE_REGISTRATION_CODE") or "1234"
ADMIN_BOOTSTRAP_PW = os.getenv("ADMIN_PASSWORD") or os.getenv("ADMIN_BOOTSTRAP_PASSWORD")


BASE_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
results = []

def record(test_name, passed, status_code, details="", is_blocked=False):
    status_label = "BLOCKED" if is_blocked else ("PASS" if passed else "FAIL")
    results.append({
        "test": test_name,
        "status": status_label,
        "passed": passed,
        "status_code": status_code,
        "details": details
    })
    print(f"[{status_label}] (HTTP {status_code}) {test_name} -> {details}")

print("=" * 70)
print("PHARMACY SYSTEM LIVE END-TO-END VERIFICATION")
print("Target Backend:", BASE_URL)
print("=" * 70)

# Check backend reachability
try:
    r_check = requests.get(f"{BASE_URL}/api/auth/me", timeout=3)
except Exception as e:
    print(f"FATAL: Backend at {BASE_URL} is unreachable: {type(e).__name__} {e}")
    sys.exit(1)

# -------------------------------------------------------------------------
# Test 1: Employee Registration with Invalid Code
# -------------------------------------------------------------------------
ts = int(time.time())
invalid_reg = {
    "firstName": "Bad",
    "lastName": f"Reg{ts}",
    "employeeId": f"BAD-{ts}",
    "employeeCode": "INVALID-REG-CODE",
    "email": f"bad{ts}@pharmacare.com",
    "phone": "9876543210",
    "password": "Password@123",
    "confirmPassword": "Password@123"
}
try:
    r1 = requests.post(f"{BASE_URL}/api/auth/register", json=invalid_reg, timeout=10)
    err_msg = r1.json().get('message', '') if r1.headers.get('content-type', '').startswith('application/json') else r1.text[:80]
    record("1. Registration with Invalid Code Rejection", r1.status_code == 401, r1.status_code, f"Response: {err_msg}")
except Exception as e:
    record("1. Registration with Invalid Code Rejection", False, 0, f"Error: {type(e).__name__} {e}")

# -------------------------------------------------------------------------
# Test 2: Employee Registration Password Mismatch
# -------------------------------------------------------------------------
mismatch_reg = {
    "firstName": "Mismatch",
    "lastName": f"User{ts}",
    "employeeId": f"MISMATCH-{ts}",
    "employeeCode": REG_CODE,
    "email": f"mismatch{ts}@pharmacare.com",
    "phone": "9876543210",
    "password": "Password@123",
    "confirmPassword": "DifferentPassword@456"
}
try:
    r2 = requests.post(f"{BASE_URL}/api/auth/register", json=mismatch_reg, timeout=10)
    err_msg = r2.json().get('message', '') if r2.headers.get('content-type', '').startswith('application/json') else r2.text[:80]
    record("2. Registration Password Mismatch Rejection", r2.status_code == 400, r2.status_code, f"Response: {err_msg}")
except Exception as e:
    record("2. Registration Password Mismatch Rejection", False, 0, f"Error: {type(e).__name__} {e}")

# -------------------------------------------------------------------------
# Test 3: Valid Employee Registration (Least-Privilege Role: CASHIER)
# -------------------------------------------------------------------------
cashier_emp_id = f"CASHIER-LIVE-{ts}"
cashier_raw_pass = "CashierPass@123"
valid_reg = {
    "firstName": "Cashier",
    "lastName": f"Live{ts}",
    "employeeId": cashier_emp_id,
    "employeeCode": REG_CODE,
    "email": f"cashier.live{ts}@pharmacare.com",
    "phone": "9876543210",
    "password": cashier_raw_pass,
    "confirmPassword": cashier_raw_pass
}
reg_passed = False
try:
    r3 = requests.post(f"{BASE_URL}/api/auth/register", json=valid_reg, timeout=10)
    body3 = r3.json() if r3.status_code == 201 else {}
    reg_passed = (
        r3.status_code == 201 and
        body3.get("role") == "CASHIER" and
        "passwordHash" not in body3 and
        body3.get("employeeId") == cashier_emp_id
    )
    msg3 = f"Assigned Role: {body3.get('role')}, Excludes PasswordHash: {'passwordHash' not in body3}" if r3.status_code == 201 else f"Failed: {r3.text[:100]}"
    record("3. Valid Employee Self-Registration (Least-Privilege Role CASHIER)", reg_passed, r3.status_code, msg3)
except Exception as e:
    record("3. Valid Employee Self-Registration (Least-Privilege Role CASHIER)", False, 0, f"Error: {type(e).__name__} {e}")

# -------------------------------------------------------------------------
# Test 4: Login with Incorrect Credentials
# -------------------------------------------------------------------------
cashier_session = requests.Session()
bad_login = {"identifier": cashier_emp_id, "password": "WrongPassword@999"}
try:
    r4 = cashier_session.post(f"{BASE_URL}/api/auth/login", json=bad_login, timeout=10)
    err_msg = r4.json().get('message', '') if r4.headers.get('content-type', '').startswith('application/json') else r4.text[:80]
    record("4. Login with Incorrect Credentials Rejection", r4.status_code == 401, r4.status_code, f"Response: {err_msg}")
except Exception as e:
    record("4. Login with Incorrect Credentials Rejection", False, 0, f"Error: {type(e).__name__} {e}")

# -------------------------------------------------------------------------
# Test 5: Login with Correct Cashier Credentials
# -------------------------------------------------------------------------
good_login = {"identifier": cashier_emp_id, "password": cashier_raw_pass}
cashier_logged_in = False
try:
    r5 = cashier_session.post(f"{BASE_URL}/api/auth/login", json=good_login, timeout=10)
    body5 = r5.json() if r5.status_code == 200 else {}
    cashier_logged_in = (
        r5.status_code == 200 and
        body5.get("employeeId") == cashier_emp_id and
        body5.get("role") == "CASHIER" and
        "JSESSIONID" in cashier_session.cookies
    )
    msg5 = f"Authenticated User: {body5.get('employeeId')}, Session Established: {'JSESSIONID' in cashier_session.cookies}" if r5.status_code == 200 else f"Failed: {r5.text[:100]}"
    record("5. Login with Correct Cashier Credentials", cashier_logged_in, r5.status_code, msg5)
except Exception as e:
    record("5. Login with Correct Cashier Credentials", False, 0, f"Error: {type(e).__name__} {e}")

# -------------------------------------------------------------------------
# Test 6: Unauthenticated Requests to Protected Endpoints Blocked
# -------------------------------------------------------------------------
unauth_session = requests.Session()
try:
    r6_bills = unauth_session.get(f"{BASE_URL}/api/bills", timeout=10)
    r6_history = unauth_session.get(f"{BASE_URL}/api/login-history", timeout=10)
    r6_activity = unauth_session.get(f"{BASE_URL}/api/activity/my", timeout=10)
    unauth_passed = (r6_bills.status_code in [401, 403] and r6_history.status_code in [401, 403] and r6_activity.status_code in [401, 403])
    record("6. Unauthenticated Access to Protected Endpoints Blocked", unauth_passed, 401, f"/api/bills: {r6_bills.status_code}, /api/login-history: {r6_history.status_code}, /api/activity/my: {r6_activity.status_code}")
except Exception as e:
    record("6. Unauthenticated Access to Protected Endpoints Blocked", False, 0, f"Error: {type(e).__name__} {e}")

# -------------------------------------------------------------------------
# Test 7: Cashier Access Restrictions on Admin-Only Endpoints
# -------------------------------------------------------------------------
if cashier_logged_in:
    try:
        r7_history = cashier_session.get(f"{BASE_URL}/api/login-history", timeout=10)
        r7_employees = cashier_session.get(f"{BASE_URL}/api/employees", timeout=10)
        cashier_restricted = (r7_history.status_code == 403 and r7_employees.status_code == 403)
        record("7. Cashier Blocked from Admin-Only Endpoints (403 Forbidden)", cashier_restricted, 403, f"/api/login-history: {r7_history.status_code}, /api/employees: {r7_employees.status_code}")
    except Exception as e:
        record("7. Cashier Blocked from Admin-Only Endpoints (403 Forbidden)", False, 0, f"Error: {type(e).__name__} {e}")
else:
    record("7. Cashier Blocked from Admin-Only Endpoints (403 Forbidden)", False, 0, "Blocked: Cashier login failed", is_blocked=True)

# -------------------------------------------------------------------------
# Test 8: Admin Authentication (Strict Environment-Configured Credentials Only)
# -------------------------------------------------------------------------
# Read strictly from environment or .env; no hard-coded password fallback
admin_id = os.getenv("ADMIN_BOOTSTRAP_ID") or os.getenv("ADMIN_ID")
admin_password = ADMIN_BOOTSTRAP_PW or os.getenv("ADMIN_BOOTSTRAP_PASSWORD")

if (not admin_id or not admin_password) and os.path.exists(".env"):
    for line in open(".env"):
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            k, v = line.split("=", 1)
            k = k.strip()
            v = v.strip().strip("'").strip('"')
            if k in ["ADMIN_BOOTSTRAP_ID", "ADMIN_ID"] and not admin_id:
                admin_id = v
            if k in ["ADMIN_PASSWORD", "ADMIN_BOOTSTRAP_PASSWORD"] and not admin_password:
                admin_password = v

admin_session = requests.Session()
admin_logged_in = False

if not admin_password:
    record("8. Admin Authentication", False, 0, "ADMIN_PASSWORD not set in environment or .env (BLOCKED/SKIPPED)", is_blocked=True)
    record("9. Admin Organization-Wide Login History with Pagination", False, 0, "Blocked: Admin session unavailable", is_blocked=True)
    record("10. Admin Cross-Employee Activity Inspection", False, 0, "Blocked: Admin session unavailable", is_blocked=True)
else:
    target_admin_id = admin_id or "EMP-001"
    try:
        r8 = admin_session.post(f"{BASE_URL}/api/auth/login", json={"identifier": target_admin_id, "password": admin_password}, timeout=10)
        body8 = r8.json() if r8.status_code == 200 else {}
        admin_logged_in = (
            r8.status_code == 200 and
            body8.get("role") == "ADMIN"
        )
        msg8 = f"Admin User: {body8.get('employeeId')}, Role: {body8.get('role')}" if r8.status_code == 200 else f"Admin login failed: Status {r8.status_code}, Body: {r8.text[:100]}"
        record("8. Admin Authentication with Configured Credentials", admin_logged_in, r8.status_code, msg8)
    except Exception as e:
        record("8. Admin Authentication with Configured Credentials", False, 0, f"Error: {type(e).__name__} {e}")

    # -------------------------------------------------------------------------
    # Test 9: Admin Organization-Wide Login History with Pagination & Filters
    # -------------------------------------------------------------------------
    if admin_logged_in:
        try:
            r9 = admin_session.get(f"{BASE_URL}/api/login-history?page=0&size=10", timeout=10)
            body9 = r9.json() if r9.status_code == 200 else {}
            items9 = body9.get("content", [])
            no_session_leaks = all("sessionReference" not in item for item in items9)
            admin_history_passed = (
                r9.status_code == 200 and
                no_session_leaks and
                "totalElements" in body9
            )
            msg9 = f"Total Audit Records: {body9.get('totalElements')}, Page Size: {len(items9)}, Session Tokens Leaked: {not no_session_leaks}" if r9.status_code == 200 else f"Failed: Status {r9.status_code}, Body: {r9.text[:100]}"
            record("9. Admin Organization-Wide Login History with Pagination", admin_history_passed, r9.status_code, msg9)
        except Exception as e:
            record("9. Admin Organization-Wide Login History with Pagination", False, 0, f"Error: {type(e).__name__} {e}")

        # ---------------------------------------------------------------------
        # Test 10: Admin Cross-Employee Activity Inspection
        # ---------------------------------------------------------------------
        try:
            r10 = admin_session.get(f"{BASE_URL}/api/activity/{cashier_emp_id}", timeout=10)
            body10 = r10.json() if r10.status_code == 200 else {}
            admin_activity_passed = (
                r10.status_code == 200 and
                body10.get("employeeId") == cashier_emp_id and
                "totalSalesValue" in body10
            )
            msg10 = f"Inspected Employee: {body10.get('employeeId')}, Bills Count: {body10.get('totalBillsCount')}" if r10.status_code == 200 else f"Failed: Status {r10.status_code}, Body: {r10.text[:100]}"
            record("10. Admin Cross-Employee Activity Inspection", admin_activity_passed, r10.status_code, msg10)
        except Exception as e:
            record("10. Admin Cross-Employee Activity Inspection", False, 0, f"Error: {type(e).__name__} {e}")
    else:
        record("9. Admin Organization-Wide Login History with Pagination", False, 0, "Blocked: Admin login failed", is_blocked=True)
        record("10. Admin Cross-Employee Activity Inspection", False, 0, "Blocked: Admin login failed", is_blocked=True)

# -------------------------------------------------------------------------
# Test 11: Bill Creation Tracks Authenticated Employee Identity
# -------------------------------------------------------------------------
if cashier_logged_in:
    target_med = None
    target_cust = None
    try:
        r_meds = cashier_session.get(f"{BASE_URL}/api/medicines", timeout=10)
        meds = r_meds.json() if r_meds.status_code == 200 else []
        target_med = next((m for m in meds if m.get("stockQuantity", 0) >= 10), None)

        r_custs = cashier_session.get(f"{BASE_URL}/api/customers", timeout=10)
        custs = r_custs.json() if r_custs.status_code == 200 else []
        target_cust = custs[0] if custs else None
    except Exception as e:
        pass

    if target_med and target_cust:
        med_id = target_med.get("medicineId") or target_med.get("id")
        cust_id = target_cust.get("customerId") or target_cust.get("id")
        initial_stock = target_med.get("stockQuantity")

        bill_payload = {
            "customerId": cust_id,
            "items": [{"medicineId": med_id, "quantity": 2}]
        }
        try:
            r11 = cashier_session.post(f"{BASE_URL}/api/bills", json=bill_payload, timeout=10)
            body11 = r11.json() if r11.status_code == 201 else {}
            bill_passed = (
                r11.status_code == 201 and
                body11.get("createdByEmployeeId") == cashier_emp_id and
                body11.get("totalAmount") is not None
            )
            msg11 = f"Bill ID: {body11.get('id')}, Creator: {body11.get('createdByEmployeeId')}, Total: ₹{body11.get('totalAmount')}" if r11.status_code == 201 else f"Failed: Status {r11.status_code}, Body: {r11.text[:100]}"
            record("11. Bill Creation Tracks Authenticated Employee Identity", bill_passed, r11.status_code, msg11)

            # -----------------------------------------------------------------
            # Test 12: Stock Reduction and Calculation Correctness
            # -----------------------------------------------------------------
            r_med_after = cashier_session.get(f"{BASE_URL}/api/medicines/{med_id}", timeout=10)
            new_stock = r_med_after.json().get("stockQuantity") if r_med_after.status_code == 200 else None
            stock_passed = (new_stock == initial_stock - 2)
            msg12 = f"Initial Stock: {initial_stock}, Decremented to: {new_stock} (-2)" if stock_passed else f"Stock check failed: was {initial_stock}, now {new_stock}"
            record("12. Stock Reduction and Calculation Correctness", stock_passed, r_med_after.status_code, msg12)

            # -----------------------------------------------------------------
            # Test 13: Insufficient Stock Rejection & Rollback Protection
            # -----------------------------------------------------------------
            oversell_payload = {
                "customerId": cust_id,
                "items": [{"medicineId": med_id, "quantity": (new_stock or 0) + 500}]
            }
            r13 = cashier_session.post(f"{BASE_URL}/api/bills", json=oversell_payload, timeout=10)
            r_med_check = cashier_session.get(f"{BASE_URL}/api/medicines/{med_id}", timeout=10)
            stock_preserved = (r_med_check.json().get("stockQuantity") == new_stock)
            oversell_passed = (r13.status_code == 400 and stock_preserved)
            msg13 = f"Status: {r13.status_code}, Stock Preserved at: {new_stock}" if oversell_passed else f"Failed: Status {r13.status_code}, Body: {r13.text[:100]}"
            record("13. Insufficient Stock Rejection & Rollback Protection", oversell_passed, r13.status_code, msg13)
        except Exception as e:
            record("11. Bill Creation Tracks Authenticated Employee Identity", False, 0, f"Error: {type(e).__name__} {e}")
            record("12. Stock Reduction and Calculation Correctness", False, 0, "Skipped due to prior failure")
            record("13. Insufficient Stock Rejection & Rollback Protection", False, 0, "Skipped due to prior failure")
    else:
        record("11. Bill Creation Tracks Authenticated Employee Identity", False, 0, "Blocked: No test medicines or customers available in database", is_blocked=True)
        record("12. Stock Reduction and Calculation Correctness", False, 0, "Blocked", is_blocked=True)
        record("13. Insufficient Stock Rejection & Rollback Protection", False, 0, "Blocked", is_blocked=True)

    # -------------------------------------------------------------------------
    # Test 14: /api/bills/my Scoped Strictly to Authenticated Employee
    # -------------------------------------------------------------------------
    try:
        r14 = cashier_session.get(f"{BASE_URL}/api/bills/my", timeout=10)
        bills14 = r14.json() if r14.status_code == 200 else []
        my_bills_scoped = (
            r14.status_code == 200 and
            all(b.get("createdByEmployeeId") == cashier_emp_id for b in bills14)
        )
        msg14 = f"Returned {len(bills14)} bill(s); All Match Creator ID: {my_bills_scoped}" if r14.status_code == 200 else f"Failed: Status {r14.status_code}, Body: {r14.text[:100]}"
        record("14. /api/bills/my Scoped Strictly to Authenticated Employee", my_bills_scoped, r14.status_code, msg14)
    except Exception as e:
        record("14. /api/bills/my Scoped Strictly to Authenticated Employee", False, 0, f"Error: {type(e).__name__} {e}")

    # -------------------------------------------------------------------------
    # Test 15: /api/activity/my Scoped Strictly to Authenticated Employee
    # -------------------------------------------------------------------------
    try:
        r15 = cashier_session.get(f"{BASE_URL}/api/activity/my", timeout=10)
        act15 = r15.json() if r15.status_code == 200 else {}
        activity_scoped = (
            r15.status_code == 200 and
            act15.get("employeeId") == cashier_emp_id and
            "totalBillsCount" in act15 and
            "totalSalesValue" in act15
        )
        msg15 = f"Employee: {act15.get('employeeId')}, Bills Count: {act15.get('totalBillsCount')}, Sales: ₹{act15.get('totalSalesValue')}" if r15.status_code == 200 else f"Failed: Status {r15.status_code}, Body: {r15.text[:100]}"
        record("15. /api/activity/my Scoped Strictly to Authenticated Employee", activity_scoped, r15.status_code, msg15)
    except Exception as e:
        record("15. /api/activity/my Scoped Strictly to Authenticated Employee", False, 0, f"Error: {type(e).__name__} {e}")

    # -------------------------------------------------------------------------
    # Test 16: Personal Login History Retrieval (Safe, No Credential/Session Leak)
    # -------------------------------------------------------------------------
    try:
        r16 = cashier_session.get(f"{BASE_URL}/api/login-history/my", timeout=10)
        body16 = r16.json() if r16.status_code == 200 else {}
        events16 = body16.get("content", [])
        no_session_leaks_16 = all("sessionReference" not in e for e in events16)
        history_scoped = (
            r16.status_code == 200 and
            no_session_leaks_16
        )
        msg16 = f"Audit Events Count: {len(events16)}, Session Tokens Leaked: {not no_session_leaks_16}" if r16.status_code == 200 else f"Failed: Status {r16.status_code}, Body: {r16.text[:100]}"
        record("16. Personal Login History (/api/login-history/my) Retrieval", history_scoped, r16.status_code, msg16)
    except Exception as e:
        record("16. Personal Login History (/api/login-history/my) Retrieval", False, 0, f"Error: {type(e).__name__} {e}")

    # -------------------------------------------------------------------------
    # Test 17: Logout Endpoint Terminates Session
    # -------------------------------------------------------------------------
    try:
        r17 = cashier_session.post(f"{BASE_URL}/api/auth/logout", timeout=10)
        r17_check = cashier_session.get(f"{BASE_URL}/api/bills/my", timeout=10)
        logout_passed = (r17.status_code == 200 and r17_check.status_code in [401, 403])
        record("17. Logout Endpoint Terminates Session & Clears Auth", logout_passed, r17.status_code, f"Subsequent Request Status: {r17_check.status_code} (Unauthorized)")
    except Exception as e:
        record("17. Logout Endpoint Terminates Session & Clears Auth", False, 0, f"Error: {type(e).__name__} {e}")
else:
    for i, name in [
        (11, "11. Bill Creation Tracks Authenticated Employee Identity"),
        (12, "12. Stock Reduction and Calculation Correctness"),
        (13, "13. Insufficient Stock Rejection & Rollback Protection"),
        (14, "14. /api/bills/my Scoped Strictly to Authenticated Employee"),
        (15, "15. /api/activity/my Scoped Strictly to Authenticated Employee"),
        (16, "16. Personal Login History (/api/login-history/my) Retrieval"),
        (17, "17. Logout Endpoint Terminates Session & Clears Auth")
    ]:
        record(name, False, 0, "Blocked: Cashier login failed", is_blocked=True)

print("=" * 70)
passed_count = sum(1 for r in results if r["status"] == "PASS")
failed_count = sum(1 for r in results if r["status"] == "FAIL")
blocked_count = sum(1 for r in results if r["status"] == "BLOCKED")
total_count = len(results)
print(f"TOTAL TESTS: {total_count} | PASSED: {passed_count} | FAILED: {failed_count} | BLOCKED: {blocked_count}")
if total_count > 0:
    print(f"PASS RATE (Excluding Blocked): {(passed_count / (total_count - blocked_count) * 100) if (total_count - blocked_count) > 0 else 0:.1f}%")
print("=" * 70)

if failed_count > 0:
    sys.exit(1)
