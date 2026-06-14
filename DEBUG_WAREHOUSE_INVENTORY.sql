-- Debug Warehouse Inventory Issue
-- Run these queries to check what's in the database

-- 1. Check all inventory records for store 6
SELECT 
    i.id,
    i.quantity,
    i.unit_price,
    i.product_id,
    p.name as product_name,
    i.branch_id,
    i.store_id,
    i.last_update
FROM inventory i
LEFT JOIN product p ON i.product_id = p.id
WHERE i.store_id = 6 OR i.branch_id IN (SELECT id FROM branch WHERE store_id = 6)
ORDER BY i.id DESC;

-- 2. Check warehouse inventory only (where branch_id IS NULL)
SELECT 
    i.id,
    i.quantity,
    i.unit_price,
    p.name as product_name,
    i.store_id
FROM inventory i
LEFT JOIN product p ON i.product_id = p.id
WHERE i.store_id = 6 AND i.branch_id IS NULL
ORDER BY i.id DESC;

-- 3. Check branch inventory (where branch_id IS NOT NULL)
SELECT 
    i.id,
    i.quantity,
    i.unit_price,
    p.name as product_name,
    b.name as branch_name,
    i.branch_id,
    i.store_id
FROM inventory i
LEFT JOIN product p ON i.product_id = p.id
LEFT JOIN branch b ON i.branch_id = b.id
WHERE i.branch_id IS NOT NULL AND b.store_id = 6
ORDER BY i.id DESC;

-- 4. Count inventory items by type for store 6
SELECT 
    CASE WHEN branch_id IS NULL THEN 'WAREHOUSE' ELSE 'BRANCH' END as inventory_type,
    COUNT(*) as count
FROM inventory i
WHERE i.store_id = 6 OR i.branch_id IN (SELECT id FROM branch WHERE store_id = 6)
GROUP BY inventory_type;

-- 5. Check if store 6 exists
SELECT id, brand, name FROM store WHERE id = 6;

-- 6. Check recently added inventory (last 10 records)
SELECT 
    i.id,
    i.quantity,
    i.unit_price,
    p.name as product_name,
    i.branch_id,
    i.store_id,
    i.last_update
FROM inventory i
LEFT JOIN product p ON i.product_id = p.id
ORDER BY i.id DESC
LIMIT 10;

-- 7. Check inventory table structure
SHOW COLUMNS FROM inventory;

-- 8. Check for any inventory records with NULL quantity or 0 quantity
SELECT 
    i.id,
    i.quantity,
    i.product_id,
    i.branch_id,
    i.store_id
FROM inventory i
WHERE i.quantity IS NULL OR i.quantity = 0
ORDER BY i.id DESC
LIMIT 20;
