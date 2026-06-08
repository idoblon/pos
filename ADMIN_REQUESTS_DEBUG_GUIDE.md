# DEBUGGING: Admin Not Receiving Registration Requests

## Problem
Database has registration requests, but admin panel shows empty list.

## Step-by-Step Debugging

### Step 1: Verify Data Exists in Database

Open MySQL and run:
```sql
USE POS;
SELECT id, store_name, status, created_at 
FROM store_registration_request 
ORDER BY created_at DESC;
```

**Expected:** Should see rows with data  
**If empty:** No data in database - need to create test request

### Step 2: Check Status Values

Run this query to see exact status values:
```sql
SELECT 
    id,
    store_name,
    CONCAT('[', status, ']') as status_display,
    LENGTH(status) as status_length,
    CHAR_LENGTH(status) as char_length
FROM store_registration_request;
```

**Look for:**
- ❌ Extra spaces: `[PENDING ]` or `[ PENDING]`
- ❌ Wrong case: `[pending]` or `[Pending]`
- ❌ Wrong value: `[PAYMENT_PENDING]`
- ✅ Correct: `[PENDING]` with length 7

**If wrong format, fix with:**
```sql
UPDATE store_registration_request
SET status = TRIM(UPPER(status));
```

### Step 3: Test Backend API Directly

**A. Test without authentication (temporarily):**

Add this to `AdminController.java` temporarily:
```java
@GetMapping("/store-requests/test")
public ResponseEntity<?> testEndpoint() {
    List<StoreRegistrationRequest> all = registrationService.getAllRequests();
    return ResponseEntity.ok(Map.of(
        "total", all.size(),
        "requests", all
    ));
}
```

Then in browser visit:
```
http://localhost:8080/api/admin/store-requests/test
```

**B. Test with authentication:**

1. Login as admin in frontend
2. Open browser DevTools (F12)
3. Go to Application/Storage → Local Storage
4. Copy the `jwt` value
5. Use Postman or curl:

```bash
curl -X GET "http://localhost:8080/api/admin/store-requests?status=PENDING" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

### Step 4: Check Backend Logs

In IntelliJ console, look for these log messages:

```
Fetching store requests with status filter: PENDING
Total requests in database: X
Filtered requests with status 'PENDING': X
```

**If you see:**
- ✅ `Total requests in database: 5` - Database has data
- ❌ `Filtered requests with status 'PENDING': 0` - Filtering issue
- ❌ `Total requests in database: 0` - No data loaded

### Step 5: Check Frontend Network Tab

1. Open browser DevTools (F12)
2. Go to Network tab
3. Reload the Registration Requests page
4. Look for request to `/api/admin/store-requests?status=PENDING`

**Check:**
- ✅ Status: 200 OK
- ✅ Response body has array of requests
- ❌ Status: 401 - Not authenticated
- ❌ Status: 403 - Not authorized (not ADMIN role)
- ❌ Status: 404 - Endpoint not found
- ❌ Response body: `[]` - No data returned

### Step 6: Common Issues and Fixes

#### Issue A: Status Mismatch
**Symptom:** Database has requests but API returns empty array

**Fix:**
```sql
-- Check current status values
SELECT DISTINCT status FROM store_registration_request;

-- Update to correct format
UPDATE store_registration_request
SET status = 'PENDING'
WHERE status IN ('pending', 'Pending', 'PAYMENT_PENDING', ' PENDING', 'PENDING ');
```

#### Issue B: Backend Not Loading Data
**Symptom:** Logs show "Total requests in database: 0"

**Possible causes:**
1. Wrong database connection
2. Table name mismatch
3. JPA not configured correctly

**Fix:**
Check `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/POS
spring.jpa.show-sql=true
```

Restart backend and check logs for SQL queries.

#### Issue C: Authentication Issues
**Symptom:** 401 or 403 errors

**Fix:**
1. Logout and login again
2. Check user role in database:
```sql
SELECT id, email, role FROM user WHERE email = 'your-admin@email.com';
```
Should show `role = 'ADMIN'`

#### Issue D: Frontend Not Sending Correct Status
**Symptom:** API works in Postman but not in UI

**Check:**
In `StoreRegistrationRequests.jsx`, verify:
```javascript
const [filter, setFilter] = useState("PENDING");  // Should be uppercase
```

### Step 7: Use Debug Endpoint

I added a debug endpoint. Test it:

1. Login as admin
2. In browser console, run:
```javascript
const jwt = localStorage.getItem('jwt');
fetch('http://localhost:8080/api/admin/store-requests/debug/all', {
  headers: { 'Authorization': `Bearer ${jwt}` }
})
.then(r => r.json())
.then(d => console.log(d));
```

This will show ALL requests in the database with their exact status values.

### Step 8: Enable SQL Logging

In `application.properties`, set:
```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.com.springboot.POS=DEBUG
```

Restart backend. You'll see actual SQL queries being executed.

### Step 9: Frontend Console Debugging

Add this to `StoreRegistrationRequests.jsx` in `fetchRequests`:

```javascript
const fetchRequests = async () => {
  setLoading(true);
  try {
    console.log('Fetching with filter:', filter);
    const res = await api.get(`/api/admin/store-requests`, {
      params: { status: filter }
    });
    console.log('API Response:', res.data);
    console.log('Number of requests:', res.data?.length);
    setRequests(Array.isArray(res.data) ? res.data : []);
  } catch (error) {
    console.error('Full error:', error);
    // ... rest of code
  }
};
```

## Quick Fix Checklist

- [ ] Database has requests (`SELECT COUNT(*) FROM store_registration_request`)
- [ ] Status values are correct (`status = 'PENDING'` not `'pending'`)
- [ ] Backend is running on port 8080
- [ ] Frontend is running on port 5173
- [ ] Logged in as ADMIN user
- [ ] JWT token is valid (check localStorage)
- [ ] No 404/401/403 errors in console
- [ ] Backend logs show correct count
- [ ] API returns data when tested directly

## Most Likely Causes

1. **Status value mismatch** (95% of cases)
   - Database has `'pending'` but code expects `'PENDING'`
   - Fix: Update database to use uppercase

2. **Wrong database**
   - Backend connected to wrong DB
   - Fix: Check application.properties

3. **No data in table**
   - Table is empty
   - Fix: Create test request

4. **Authentication issue**
   - Not logged in as ADMIN
   - Fix: Check user role in database

## Next Steps

1. Run the database queries from `DATABASE_DEBUG_QUERIES.sql`
2. Check backend logs when accessing the page
3. Check frontend Network tab
4. Test the debug endpoint
5. Report back what you find!
