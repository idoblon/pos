# Branch inventory integrity migration

`src/main/resources/db/migration/V5__merge_duplicate_branch_inventory_and_add_unique_constraint.sql`
is intentionally not run by the application: this project does not currently include a migration runner, and it must be applied to the existing MySQL database before deploying the code change.

The script keeps the oldest inventory row for each duplicate `(branch_id, product_id)` pair, sums its stock, keeps the latest non-null unit price, repoints stock-movement history to that row, removes the redundant rows, and then creates `uk_inventory_branch_product`.

Before running it, take a database backup and inspect the affected records:

```sql
SELECT branch_id, product_id, COUNT(*) AS row_count, SUM(quantity) AS total_quantity
FROM inventory
WHERE branch_id IS NOT NULL
GROUP BY branch_id, product_id
HAVING COUNT(*) > 1;
```

Apply the migration during a maintenance window with the same credentials used by the service:

```powershell
mysql -u root -p POS < src/main/resources/db/migration/V5__merge_duplicate_branch_inventory_and_add_unique_constraint.sql
```

Verify that no duplicates remain afterward:

```sql
SELECT branch_id, product_id, COUNT(*) AS row_count
FROM inventory
WHERE branch_id IS NOT NULL
GROUP BY branch_id, product_id
HAVING COUNT(*) > 1;
```
