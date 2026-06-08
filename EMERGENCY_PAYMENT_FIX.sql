-- EMERGENCY FIX: Store Registration Payment Issue
-- Run these SQL commands to immediately fix the payment issue

-- Step 1: Check current status of problematic store registration
SELECT 
    r.id as registration_id,
    r.store_name,
    r.owner_name,
    r.email,
    r.status as registration_status,
    r.payment_status as reg_payment_status,
    r.subscription_plan,
    r.subscription_amount,
    r.created_at,
    p.id as payment_id,
    p.payment_status as actual_payment_status,
    p.transaction_id,
    p.amount as payment_amount,
    p.payment_method,
    p.created_at as payment_created_at,
    p.paid_at
FROM store_registration_request r
LEFT JOIN subscription_payment p ON p.registration_request_id = r.id
WHERE r.status IN ('PENDING', 'PAYMENT_PENDING', 'APPROVED')
   OR r.payment_status = 'PENDING'
ORDER BY r.created_at DESC;

-- Step 2: Find the specific registration that needs fixing
-- Replace {REGISTRATION_ID} with the actual registration ID from Step 1
-- SET @registration_id = {REGISTRATION_ID};  -- Uncomment and set the actual ID

-- Step 3: Check if payment record exists
-- SELECT * FROM subscription_payment WHERE registration_request_id = @registration_id;

-- Step 4A: If payment record EXISTS, mark it as completed
/*
UPDATE subscription_payment 
SET 
    payment_status = 'COMPLETED',
    paid_at = NOW(),
    payment_gateway_reference = CONCAT('MANUAL_FIX_', UNIX_TIMESTAMP(NOW())),
    payment_gateway_response = 'Manually marked as completed by admin for urgent approval'
WHERE registration_request_id = @registration_id;
*/

-- Step 4B: If payment record DOES NOT EXIST, create one
/*
INSERT INTO subscription_payment (
    registration_request_id,
    subscription_plan,
    amount,
    currency,
    payment_method,
    payment_status,
    transaction_id,
    payment_gateway_reference,
    payment_gateway_response,
    created_at,
    paid_at,
    expires_at,
    subscription_start_date,
    subscription_end_date,
    is_recurring
) 
SELECT 
    r.id,
    r.subscription_plan,
    CASE 
        WHEN r.subscription_plan = 'BASIC' THEN 2999.0
        WHEN r.subscription_plan = 'PROFESSIONAL' THEN 5999.0
        WHEN r.subscription_plan = 'ENTERPRISE' THEN 12999.0
        ELSE 2999.0
    END,
    'NPR',
    'ADMIN_OVERRIDE',
    'COMPLETED',
    CONCAT('MANUAL_', UNIX_TIMESTAMP(NOW()), '_', r.id),
    CONCAT('MANUAL_FIX_', UNIX_TIMESTAMP(NOW())),
    'Manually created and completed by admin for urgent approval',
    NOW(),
    NOW(),
    DATE_ADD(NOW(), INTERVAL 1 DAY),
    NOW(),
    DATE_ADD(NOW(), INTERVAL 1 MONTH),
    1
FROM store_registration_request r 
WHERE r.id = @registration_id;
*/

-- Step 5: Update registration request status
/*
UPDATE store_registration_request 
SET 
    payment_status = 'COMPLETED',
    status = 'PENDING'  -- Ready for admin approval
WHERE id = @registration_id;
*/

-- Step 6: Verify the fix
/*
SELECT 
    r.id as registration_id,
    r.store_name,
    r.status as registration_status,
    r.payment_status as reg_payment_status,
    p.payment_status as actual_payment_status,
    p.transaction_id,
    p.paid_at
FROM store_registration_request r
LEFT JOIN subscription_payment p ON p.registration_request_id = r.id
WHERE r.id = @registration_id;
*/

-- ================================
-- QUICK FIX COMMANDS (Replace {ID} with actual registration ID)
-- ================================

-- FOR IMMEDIATE FIX: Replace {REGISTRATION_ID} with the actual ID and run these:

/*
-- Example for registration ID 123:
UPDATE subscription_payment 
SET payment_status = 'COMPLETED', paid_at = NOW(), payment_gateway_reference = 'MANUAL_ADMIN_FIX'
WHERE registration_request_id = 123;

UPDATE store_registration_request 
SET payment_status = 'COMPLETED', status = 'PENDING'
WHERE id = 123;

-- Then try approval again from admin panel
*/

-- ================================
-- PREVENTION: Add admin user if missing
-- ================================

-- Check if admin user exists
SELECT * FROM user WHERE role = 'ROLE_ADMIN';

-- If no admin exists, create one (replace password with bcrypt hash)
/*
INSERT INTO user (name, email, password, role, created_date) 
VALUES ('System Admin', 'admin@pos.com', '$2a$10$encrypted_password_hash', 'ROLE_ADMIN', NOW());
*/

-- ================================
-- MONITORING QUERIES
-- ================================

-- Check all pending registrations with payment issues
SELECT 
    COUNT(*) as pending_registrations,
    SUM(CASE WHEN payment_status = 'PENDING' THEN 1 ELSE 0 END) as pending_payments,
    SUM(CASE WHEN payment_status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_payments
FROM store_registration_request 
WHERE status IN ('PENDING', 'PAYMENT_PENDING');

-- Check payment gateway health
SELECT 
    payment_method,
    payment_status,
    COUNT(*) as count,
    MIN(created_at) as oldest,
    MAX(created_at) as newest
FROM subscription_payment 
GROUP BY payment_method, payment_status
ORDER BY payment_method, payment_status;