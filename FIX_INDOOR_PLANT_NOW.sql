-- IMMEDIATE FIX: Update Indoor Plant World subscription to ENTERPRISE
-- Run these commands in your MySQL database

-- Step 1: Check current value
SELECT id, brand, subscription_plan, estimated_branches, estimated_users 
FROM store 
WHERE id = 1;

-- Step 2: Update to ENTERPRISE
UPDATE store 
SET subscription_plan = 'ENTERPRISE',
    updated_at = NOW()
WHERE id = 1;

-- Step 3: Verify the update worked
SELECT id, brand, subscription_plan, estimated_branches, estimated_users, updated_at 
FROM store 
WHERE id = 1;

-- Expected result after update:
-- id | brand              | subscription_plan | estimated_branches | estimated_users | updated_at
-- 1  | Indoor Plant World | ENTERPRISE        | ...                | ...             | 2024-...
