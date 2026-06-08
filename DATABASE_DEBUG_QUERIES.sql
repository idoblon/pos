# Database Debugging Queries for Store Registration Requests

## Check if table exists
```sql
USE POS;
SHOW TABLES LIKE 'store_registration_request';
```

## Check all registration requests
```sql
SELECT 
    id,
    store_name,
    owner_name,
    email,
    status,
    payment_status,
    created_at,
    processed_at
FROM store_registration_request
ORDER BY created_at DESC;
```

## Check status values (to see exact format)
```sql
SELECT DISTINCT status, COUNT(*) as count
FROM store_registration_request
GROUP BY status;
```

## Check for PENDING requests specifically
```sql
SELECT *
FROM store_registration_request
WHERE status = 'PENDING'
ORDER BY created_at DESC;
```

## Check with case-insensitive search
```sql
SELECT *
FROM store_registration_request
WHERE LOWER(status) = 'pending'
ORDER BY created_at DESC;
```

## Update status if needed (ONLY if status is wrong format)
```sql
-- Check current status format
SELECT id, store_name, CONCAT('[', status, ']') as status_with_brackets
FROM store_registration_request;

-- If needed, update to correct format (BE CAREFUL!)
UPDATE store_registration_request
SET status = 'PENDING'
WHERE LOWER(TRIM(status)) = 'pending';
```

## Check complete request details
```sql
SELECT 
    id,
    owner_name,
    email,
    phone,
    store_name,
    store_description,
    store_type,
    store_address,
    subscription_plan,
    subscription_amount,
    payment_status,
    payment_method,
    transaction_id,
    status,
    rejection_reason,
    created_at,
    processed_at,
    approved_by_admin_id,
    created_store_id,
    created_user_id
FROM store_registration_request
ORDER BY created_at DESC
LIMIT 5;
```

## Common Issues and Fixes

### Issue 1: Status has extra spaces
```sql
-- Check for spaces
SELECT id, CONCAT('[', status, ']') as status_display, LENGTH(status) as length
FROM store_registration_request;

-- Fix: Trim spaces
UPDATE store_registration_request
SET status = TRIM(status)
WHERE status != TRIM(status);
```

### Issue 2: Status is lowercase
```sql
-- Check case
SELECT status, COUNT(*) 
FROM store_registration_request 
GROUP BY status;

-- Fix: Update to uppercase
UPDATE store_registration_request
SET status = UPPER(status);
```

### Issue 3: Check if data exists at all
```sql
SELECT COUNT(*) as total_requests FROM store_registration_request;
```

## Quick Test Data Insert (if no data exists)
```sql
INSERT INTO store_registration_request (
    owner_name,
    email,
    phone,
    password,
    store_name,
    store_description,
    store_type,
    store_address,
    subscription_plan,
    subscription_amount,
    payment_status,
    status,
    created_at
) VALUES (
    'Test Owner',
    'testowner@example.com',
    '1234567890',
    '$2a$10$dummyHashedPassword',
    'Test Store',
    'A test store for debugging',
    'RETAIL',
    '123 Test Street, Test City',
    'BASIC',
    999.00,
    'COMPLETED',
    'PENDING',
    NOW()
);
```

## After Running Queries

1. Check how many PENDING requests exist
2. Verify the exact status string (check for spaces, case, etc.)
3. If status format is wrong, update it
4. Restart backend server
5. Test frontend again
