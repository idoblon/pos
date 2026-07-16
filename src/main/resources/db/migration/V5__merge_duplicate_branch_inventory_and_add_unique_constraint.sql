-- Merge legacy duplicate inventory rows before enforcing one row per branch/product.
-- Keep the oldest row, preserve the total quantity, and retain the most recently
-- updated non-null unit price.

UPDATE inventory survivor
JOIN (
    SELECT branch_id,
           product_id,
           MIN(id) AS survivor_id,
           SUM(quantity) AS merged_quantity,
           SUBSTRING_INDEX(
               GROUP_CONCAT(unit_price ORDER BY last_update DESC, id DESC), ',', 1
           ) AS merged_unit_price
    FROM inventory
    WHERE branch_id IS NOT NULL
    GROUP BY branch_id, product_id
    HAVING COUNT(*) > 1
) duplicates ON duplicates.survivor_id = survivor.id
SET survivor.quantity = duplicates.merged_quantity,
    survivor.unit_price = COALESCE(duplicates.merged_unit_price, survivor.unit_price),
    survivor.last_update = NOW();

-- Preserve stock-movement history before removing redundant inventory rows.
UPDATE stock_movement movement
JOIN inventory duplicate_row ON duplicate_row.id = movement.inventory_id
JOIN (
    SELECT branch_id, product_id, MIN(id) AS survivor_id
    FROM inventory
    WHERE branch_id IS NOT NULL
    GROUP BY branch_id, product_id
    HAVING COUNT(*) > 1
) duplicates
    ON duplicate_row.branch_id = duplicates.branch_id
    AND duplicate_row.product_id = duplicates.product_id
    AND duplicate_row.id <> duplicates.survivor_id
SET movement.inventory_id = duplicates.survivor_id;

DELETE duplicate_row
FROM inventory duplicate_row
JOIN (
    SELECT branch_id, product_id, MIN(id) AS survivor_id
    FROM inventory
    WHERE branch_id IS NOT NULL
    GROUP BY branch_id, product_id
    HAVING COUNT(*) > 1
) duplicates
    ON duplicate_row.branch_id = duplicates.branch_id
    AND duplicate_row.product_id = duplicates.product_id
    AND duplicate_row.id <> duplicates.survivor_id;

ALTER TABLE inventory
    ADD CONSTRAINT uk_inventory_branch_product UNIQUE (branch_id, product_id);
