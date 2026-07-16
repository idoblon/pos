-- Step 1: Preview duplicates
SELECT store_id, payment_type, COUNT(*) as cnt
FROM store_payment_config
GROUP BY store_id, payment_type
HAVING COUNT(*) > 1;

-- Step 2: Delete duplicates, keeping the row with the lowest id per (store_id, payment_type)
DELETE FROM store_payment_config
WHERE id NOT IN (
    SELECT min_id FROM (
        SELECT MIN(id) AS min_id
        FROM store_payment_config
        GROUP BY store_id, payment_type
    ) AS keepers
);

-- Step 3: Verify no duplicates remain
SELECT store_id, payment_type, COUNT(*) as cnt
FROM store_payment_config
GROUP BY store_id, payment_type
HAVING COUNT(*) > 1;
