-- ========================================
-- Warehouse Inventory Database Migration
-- ========================================
-- Purpose: Add support for Store Admin warehouse inventory
-- Date: 2024
-- Version: 2.0
-- ========================================

-- Step 1: Add new columns to inventory table
-- ========================================

-- Add unit_price column for inventory value calculation
ALTER TABLE inventory 
ADD COLUMN unit_price DOUBLE DEFAULT NULL 
COMMENT 'Unit price at time of adding to inventory';

-- Add store_id column for warehouse inventory (when branch_id is NULL)
ALTER TABLE inventory 
ADD COLUMN store_id BIGINT DEFAULT NULL 
COMMENT 'Store reference for warehouse inventory (NULL for branch inventory)';

-- Step 2: Add foreign key constraint
-- ========================================

ALTER TABLE inventory
ADD CONSTRAINT fk_inventory_store 
FOREIGN KEY (store_id) REFERENCES store(id) 
ON DELETE CASCADE;

-- Step 3: Add index for performance
-- ========================================

-- Index for querying warehouse inventory by store
CREATE INDEX idx_inventory_store_id ON inventory(store_id);

-- Index for composite query (warehouse + product)
CREATE INDEX idx_inventory_store_product ON inventory(store_id, product_id);

-- Index for querying where branch is null (warehouse inventory)
CREATE INDEX idx_inventory_branch_null ON inventory(branch_id) WHERE branch_id IS NULL;

-- Step 4: Populate unit_price for existing inventory (OPTIONAL)
-- ========================================

-- Update existing inventory items with current product selling price
UPDATE inventory i
INNER JOIN product p ON i.product_id = p.id
SET i.unit_price = p.selling_price
WHERE i.unit_price IS NULL;

-- Step 5: Verify the changes
-- ========================================

-- Check table structure
DESCRIBE inventory;

-- Expected columns:
-- id, branch_id, product_id, quantity, last_update, unit_price, store_id

-- Check for warehouse inventory (should be empty initially)
SELECT COUNT(*) as warehouse_inventory_count 
FROM inventory 
WHERE branch_id IS NULL AND store_id IS NOT NULL;

-- Check existing branch inventory (should match previous count)
SELECT COUNT(*) as branch_inventory_count 
FROM inventory 
WHERE branch_id IS NOT NULL;

-- ========================================
-- Rollback Script (Use if needed)
-- ========================================

/*
-- Drop foreign key
ALTER TABLE inventory DROP FOREIGN KEY fk_inventory_store;

-- Drop indexes
DROP INDEX idx_inventory_store_id ON inventory;
DROP INDEX idx_inventory_store_product ON inventory;
DROP INDEX idx_inventory_branch_null ON inventory;

-- Drop columns
ALTER TABLE inventory DROP COLUMN store_id;
ALTER TABLE inventory DROP COLUMN unit_price;
*/

-- ========================================
-- Data Integrity Checks
-- ========================================

-- Ensure all inventory records have either branch_id OR store_id (not both, not neither)
SELECT 
    COUNT(*) as total_records,
    SUM(CASE WHEN branch_id IS NULL AND store_id IS NULL THEN 1 ELSE 0 END) as invalid_null_both,
    SUM(CASE WHEN branch_id IS NOT NULL AND store_id IS NOT NULL THEN 1 ELSE 0 END) as invalid_both_set,
    SUM(CASE WHEN branch_id IS NULL AND store_id IS NOT NULL THEN 1 ELSE 0 END) as valid_warehouse,
    SUM(CASE WHEN branch_id IS NOT NULL AND store_id IS NULL THEN 1 ELSE 0 END) as valid_branch
FROM inventory;

-- Expected results:
-- invalid_null_both: 0
-- invalid_both_set: 0
-- valid_warehouse: Initially 0 (will increase as warehouse inventory is added)
-- valid_branch: All existing inventory

-- ========================================
-- Test Queries
-- ========================================

-- Test 1: Get warehouse inventory for store ID 1
SELECT 
    i.id,
    i.store_id,
    i.product_id,
    p.name as product_name,
    i.quantity,
    i.unit_price,
    (i.quantity * i.unit_price) as total_value
FROM inventory i
INNER JOIN product p ON i.product_id = p.id
WHERE i.store_id = 1 AND i.branch_id IS NULL;

-- Test 2: Get all inventory for store ID 1 (warehouse + branches)
SELECT 
    i.id,
    i.branch_id,
    b.name as branch_name,
    i.store_id,
    i.product_id,
    p.name as product_name,
    i.quantity,
    i.unit_price,
    CASE 
        WHEN i.branch_id IS NULL THEN 'WAREHOUSE'
        ELSE 'BRANCH'
    END as inventory_type
FROM inventory i
LEFT JOIN branch b ON i.branch_id = b.id
INNER JOIN product p ON i.product_id = p.id
WHERE i.store_id = 1 OR b.store_id = 1
ORDER BY inventory_type, product_name;

-- Test 3: Calculate total warehouse value for a store
SELECT 
    i.store_id,
    s.brand as store_name,
    COUNT(*) as product_count,
    SUM(i.quantity) as total_units,
    SUM(i.quantity * i.unit_price) as total_value
FROM inventory i
INNER JOIN store s ON i.store_id = s.id
WHERE i.branch_id IS NULL
GROUP BY i.store_id, s.brand;

-- Test 4: Low stock warehouse items (threshold = 10)
SELECT 
    i.id,
    p.name as product_name,
    i.quantity,
    i.unit_price
FROM inventory i
INNER JOIN product p ON i.product_id = p.id
WHERE i.store_id = 1 
  AND i.branch_id IS NULL 
  AND i.quantity <= 10
ORDER BY i.quantity ASC;

-- ========================================
-- Insert Test Data (OPTIONAL - for testing)
-- ========================================

/*
-- Insert warehouse inventory for Store ID 1, Product ID 1
INSERT INTO inventory (branch_id, store_id, product_id, quantity, unit_price, last_update)
VALUES (NULL, 1, 1, 100, 500.00, NOW());

-- Insert warehouse inventory for Store ID 1, Product ID 2
INSERT INTO inventory (branch_id, store_id, product_id, quantity, unit_price, last_update)
VALUES (NULL, 1, 2, 200, 800.00, NOW());

-- Verify insertion
SELECT * FROM inventory WHERE store_id = 1 AND branch_id IS NULL;
*/

-- ========================================
-- Performance Optimization Queries
-- ========================================

-- Analyze table for query optimization
ANALYZE TABLE inventory;

-- Check index usage
SHOW INDEX FROM inventory;

-- ========================================
-- Migration Complete
-- ========================================

-- Summary:
-- ✅ Added unit_price column
-- ✅ Added store_id column  
-- ✅ Added foreign key constraint
-- ✅ Added indexes for performance
-- ✅ Populated unit_price for existing records (optional)
-- ✅ Verified data integrity

SELECT 'Warehouse Inventory Migration Completed Successfully!' as status;
