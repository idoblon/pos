-- Fix for Safe Update Mode Error
-- This script will update status values correctly

USE POS;

-- Temporarily disable safe update mode
SET SQL_SAFE_UPDATES = 0;

-- Show current status values
SELECT 'BEFORE UPDATE:' as Status;
SELECT 
    id,
    store_name,
    CONCAT('[', status, ']') as current_status,
    LENGTH(status) as status_length
FROM store_registration_request
ORDER BY id;

-- Update status values to correct format
UPDATE store_registration_request
SET status = TRIM(UPPER(status))
WHERE id > 0;  -- This WHERE clause satisfies safe mode

-- Show updated status values
SELECT 'AFTER UPDATE:' as Status;
SELECT 
    id,
    store_name,
    CONCAT('[', status, ']') as new_status,
    LENGTH(status) as status_length
FROM store_registration_request
ORDER BY id;

-- Verify status distribution
SELECT 'STATUS SUMMARY:' as Info;
SELECT status, COUNT(*) as count
FROM store_registration_request
GROUP BY status;

-- Re-enable safe update mode (good practice)
SET SQL_SAFE_UPDATES = 1;

-- Show all PENDING requests
SELECT 'PENDING REQUESTS:' as Info;
SELECT 
    id,
    store_name,
    owner_name,
    email,
    status,
    payment_status,
    created_at
FROM store_registration_request
WHERE status = 'PENDING'
ORDER BY created_at DESC;

SELECT 'Fix completed! Status values are now in correct format (PENDING, APPROVED, REJECTED)' as Result;
