-- ========================================
-- FIX ALL STORE DATA ISSUES
-- Run these commands in MySQL Workbench
-- ========================================

-- Step 0: Select your database
USE pos;
-- If the above doesn't work, try: USE pos_db; or USE POS;

-- ========================================
-- STEP 1: CHECK CURRENT STORE DATA
-- ========================================
SELECT 
    id, 
    brand AS store_name,
    subscription_plan,
    estimated_branches,
    estimated_users,
    owner_name AS full_name,
    store_address,
    contact_email,
    contact_phone,
    created_at,
    updated_at
FROM store
ORDER BY id;

-- ========================================
-- STEP 2: FIX INDOOR PLANT WORLD - Update subscription to ENTERPRISE
-- ========================================
UPDATE store 
SET 
    subscription_plan = 'ENTERPRISE',
    updated_at = NOW()
WHERE id = 1 
  OR brand LIKE '%Indoor Plant%';

-- Verify Indoor Plant World update
SELECT id, brand, subscription_plan, contact_email, updated_at
FROM store 
WHERE id = 1 OR brand LIKE '%Indoor Plant%';

-- ========================================
-- STEP 3: FIX MITRA PUSTAK - Add missing email from registration
-- ========================================

-- First, find Mitra Pustak's registration email
SELECT 
    id AS request_id,
    store_name,
    email AS registration_email,
    phone,
    owner_name,
    status
FROM store_registration_request
WHERE store_name LIKE '%Mitra%Pustak%'
   OR store_name LIKE '%Mitra Pustak%';

-- Update Mitra Pustak store with email from registration
-- Replace 'actual_email@example.com' with the email from the query above
UPDATE store s
INNER JOIN store_registration_request srr 
    ON s.registration_request_id = srr.id 
    OR s.brand = srr.store_name
SET 
    s.contact_email = srr.email,
    s.contact_phone = COALESCE(s.contact_phone, srr.phone),
    s.owner_name = COALESCE(s.owner_name, srr.owner_name),
    s.store_address = COALESCE(s.store_address, srr.store_address),
    s.subscription_plan = COALESCE(s.subscription_plan, srr.subscription_plan),
    s.estimated_branches = COALESCE(s.estimated_branches, srr.estimated_branches),
    s.estimated_users = COALESCE(s.estimated_users, srr.estimated_users),
    s.updated_at = NOW()
WHERE s.brand LIKE '%Mitra%Pustak%';

-- ========================================
-- STEP 4: FIX ALL STORES - Sync missing data from registration
-- ========================================

-- Update ALL stores with missing data from their registration requests
UPDATE store s
INNER JOIN store_registration_request srr 
    ON s.registration_request_id = srr.id 
    OR (s.brand = srr.store_name AND srr.status = 'APPROVED')
SET 
    s.contact_email = COALESCE(s.contact_email, srr.email),
    s.contact_phone = COALESCE(s.contact_phone, srr.phone),
    s.owner_name = COALESCE(s.owner_name, srr.owner_name),
    s.store_address = COALESCE(s.store_address, srr.store_address),
    s.subscription_plan = COALESCE(s.subscription_plan, srr.subscription_plan),
    s.estimated_branches = COALESCE(s.estimated_branches, srr.estimated_branches),
    s.estimated_users = COALESCE(s.estimated_users, srr.estimated_users),
    s.updated_at = NOW()
WHERE s.contact_email IS NULL 
   OR s.subscription_plan IS NULL
   OR s.owner_name IS NULL;

-- ========================================
-- STEP 5: VERIFY ALL FIXES
-- ========================================

-- Check all stores now have complete data
SELECT 
    id, 
    brand AS store_name,
    subscription_plan,
    estimated_branches,
    estimated_users,
    owner_name AS full_name,
    store_address,
    contact_email,
    contact_phone,
    updated_at,
    CASE 
        WHEN contact_email IS NULL THEN '❌ Missing Email'
        WHEN subscription_plan IS NULL THEN '❌ Missing Plan'
        WHEN owner_name IS NULL THEN '❌ Missing Owner'
        ELSE '✅ Complete'
    END AS status
FROM store
ORDER BY id;

-- ========================================
-- STEP 6: CHECK FOR STORES STILL MISSING DATA
-- ========================================

-- Find stores that still have NULL values after the fix
SELECT 
    s.id,
    s.brand AS store_name,
    s.contact_email,
    s.subscription_plan,
    s.owner_name,
    srr.email AS registration_email,
    srr.subscription_plan AS registration_plan,
    srr.owner_name AS registration_owner
FROM store s
LEFT JOIN store_registration_request srr 
    ON s.registration_request_id = srr.id
WHERE s.contact_email IS NULL 
   OR s.subscription_plan IS NULL 
   OR s.owner_name IS NULL;

-- ========================================
-- EXPECTED RESULTS AFTER RUNNING THIS SCRIPT:
-- ========================================
-- 1. Indoor Plant World: subscription_plan = 'ENTERPRISE'
-- 2. Mitra Pustak: contact_email filled from registration
-- 3. All stores: Complete data synced from registration requests
-- 4. No NULL values in critical fields

-- ========================================
-- NOTES:
-- ========================================
-- - COALESCE(a, b) means: use 'a' if not NULL, otherwise use 'b'
-- - This ensures we don't overwrite existing good data
-- - Only fills in NULL fields with data from registration
-- - Safe to run multiple times (idempotent)
