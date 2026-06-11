-- Update Mitra Pustak subscription plan to PROFESSIONAL
-- Run this SQL in your MySQL database

UPDATE store 
SET 
    subscription_plan = 'PROFESSIONAL',
    estimated_branches = 10,
    estimated_users = 50,
    owner_name = 'Mitra Pustak Owner'
WHERE brand = 'Mitra Pustak';

-- Verify the update
SELECT id, brand, subscription_plan, estimated_branches, estimated_users, owner_name 
FROM store 
WHERE brand = 'Mitra Pustak';
