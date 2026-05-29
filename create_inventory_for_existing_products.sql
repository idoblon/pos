-- Create inventory records for all existing products in all branches
-- This ensures every branch has inventory for every product in their store

INSERT INTO inventory (product_id, branch_id, quantity, last_update)
SELECT p.id, b.id, 0, NOW()
FROM product p
CROSS JOIN branch b
WHERE p.store_id = b.store_id
  AND p.deleted = false
  AND b.deleted = false
  AND NOT EXISTS (
    SELECT 1 FROM inventory i 
    WHERE i.product_id = p.id AND i.branch_id = b.id
  );

-- Verify the inventory records created
SELECT 
    b.name AS branch_name,
    p.name AS product_name,
    i.quantity,
    i.last_update
FROM inventory i
JOIN branch b ON i.branch_id = b.id
JOIN product p ON i.product_id = p.id
ORDER BY b.name, p.name;
