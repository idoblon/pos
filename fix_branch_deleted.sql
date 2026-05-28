-- Disable safe update mode
SET SQL_SAFE_UPDATES = 0;

-- Fix NULL deleted values in all tables
UPDATE branch SET deleted = false WHERE deleted IS NULL;
UPDATE product SET deleted = false WHERE deleted IS NULL;
UPDATE category SET deleted = false WHERE deleted IS NULL;
UPDATE user SET deleted = false WHERE deleted IS NULL;
UPDATE store SET deleted = false WHERE deleted IS NULL;
UPDATE customer SET deleted = false WHERE deleted IS NULL;

-- Re-enable safe update mode
SET SQL_SAFE_UPDATES = 1;

-- Verify the updates
SELECT 'branch' as table_name, COUNT(*) as fixed_count FROM branch WHERE deleted = false
UNION ALL
SELECT 'product', COUNT(*) FROM product WHERE deleted = false
UNION ALL
SELECT 'category', COUNT(*) FROM category WHERE deleted = false
UNION ALL
SELECT 'user', COUNT(*) FROM user WHERE deleted = false
UNION ALL
SELECT 'store', COUNT(*) FROM store WHERE deleted = false
UNION ALL
SELECT 'customer', COUNT(*) FROM customer WHERE deleted = false;
