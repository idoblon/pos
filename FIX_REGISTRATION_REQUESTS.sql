-- Quick Fix Script for Store Registration Requests
-- Run this to diagnose and fix common issues

USE POS;

-- ========================================
-- STEP 1: Check if table exists
-- ========================================
SELECT 'Checking if table exists...' as Step;
SELECT TABLE_NAME, TABLE_ROWS 
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'POS' 
AND TABLE_NAME = 'store_registration_request';

-- ========================================
-- STEP 2: Count total requests
-- ========================================
SELECT 'Total requests in database:' as Step;
SELECT COUNT(*) as total_count FROM store_registration_request;

-- ========================================
-- STEP 3: Check status values (THIS IS THE ISSUE 95% OF THE TIME)
-- ========================================
SELECT 'Current status values:' as Step;
SELECT 
    status,
    COUNT(*) as count,
    CONCAT('[', status, ']') as status_with_brackets,
    LENGTH(status) as length
FROM store_registration_request
GROUP BY status;

-- ========================================
-- STEP 4: Show all requests with details
-- ========================================
SELECT 'All requests:' as Step;
SELECT 
    id,
    store_name,
    owner_name,
    email,
    CONCAT('[', status, ']') as status_display,
    payment_status,
    DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') as created,
    DATE_FORMAT(processed_at, '%Y-%m-%d %H:%i:%s') as processed
FROM store_registration_request
ORDER BY created_at DESC;

-- ========================================
-- STEP 5: FIX - Update status to correct format
-- ========================================
-- ONLY RUN THIS IF status values are wrong (lowercase, spaces, etc.)

-- Uncomment the lines below to fix status values:

-- UPDATE store_registration_request
-- SET status = TRIM(UPPER(status))
-- WHERE status != TRIM(UPPER(status));

-- SELECT 'Status values have been fixed!' as Result;

-- ========================================
-- STEP 6: Verify fix worked
-- ========================================
SELECT 'Status values after fix:' as Step;
SELECT 
    status,
    COUNT(*) as count
FROM store_registration_request
GROUP BY status;

-- ========================================
-- STEP 7: Show PENDING requests specifically
-- ========================================
SELECT 'PENDING requests:' as Step;
SELECT 
    id,
    store_name,
    owner_name,
    email,
    subscription_plan,
    created_at
FROM store_registration_request
WHERE status = 'PENDING'
ORDER BY created_at DESC;

-- ========================================
-- INSTRUCTIONS:
-- ========================================
-- 1. Run this entire script
-- 2. Check the output
-- 3. If status values are wrong (lowercase, spaces), uncomment the UPDATE line and run again
-- 4. Restart your backend server
-- 5. Refresh admin panel
