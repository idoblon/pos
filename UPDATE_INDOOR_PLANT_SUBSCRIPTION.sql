-- Update Indoor Plant World store subscription plan to ENTERPRISE
-- Run this SQL to manually fix the subscription plan

-- First, find the Indoor Plant World store ID
SELECT id, brand, subscription_plan, estimated_branches, estimated_users
FROM store
WHERE brand LIKE '%Indoor Plant%';

-- Update the subscription plan to ENTERPRISE
UPDATE store
SET subscription_plan = 'ENTERPRISE',
    updated_at = CURRENT_TIMESTAMP
WHERE brand LIKE '%Indoor Plant%';

-- Verify the update
SELECT id, brand, subscription_plan, estimated_branches, estimated_users, updated_at
FROM store
WHERE brand LIKE '%Indoor Plant%';
