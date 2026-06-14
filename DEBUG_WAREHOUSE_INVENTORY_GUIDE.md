# Debug Guide: Warehouse Inventory Shows 0

## Problem
Even after adding a product to warehouse, the warehouse inventory list shows 0 items.

## Solution: Multi-Step Debugging

### Step 1: Check Backend Console Logs
After rebuilding the backend with the debug logging added:

1. **Restart backend** - `mvn spring-boot:run`
2. **Open backend console**
3. **Try adding a product to warehouse** in frontend
4. **Check console for these logs:**

```
=== CREATING INVENTORY ===
DTO: branchId=null, storeId=6
DTO: productId=207, quantity=50
Creating WAREHOUSE inventory...
Warehouse inventory built: 50 units
Before save - Quantity: 50
After save - ID: 1, Quantity: 50
Returning DTO...
DTO returned with quantity: 50
=== INVENTORY CREATED SUCCESSFULLY ===

[Frontend logs]
✅ Product added, now fetching updated inventory
📡 CONTROLLER: GET /api/inventories/store/6
📡 SERVICE: getAllInventoryByStoreId for store: 6
📡 SERVICE: Found 1 inventory items
  - ID: 1, Product: Palpasa Café, BranchId: NULL, Qty: 50
🌐 CONTROLLER: Returning 1 items
```

**What to look for:**
- Is `Creating WAREHOUSE inventory...` showing?
- Is quantity saved correctly?
- Is the GET request returning items?

### Step 2: Check Database Directly
Run the SQL queries from `DEBUG_WAREHOUSE_INVENTORY.sql`:

**Most important query:**
```sql
SELECT * FROM inventory 
WHERE store_id = 6 AND branch_id IS NULL
ORDER BY id DESC;
```

**Expected result:**
```
| id | quantity | unit_price | product_id | store_id | branch_id | last_update       |
|----|----------|-----------|------------|----------|-----------|------------------|
| 1  | 50       | 300       | 207        | 6        | NULL      | 2024-01-15 10:30 |
```

**If you see:**
- ✅ **Rows with data** → Database is saving correctly, issue is in API/Frontend
- ❌ **0 rows** → Data is not being saved, issue is in service/entity

### Step 3: Test API Directly with Postman/Browser

**Test 1: Add Inventory**
```
POST http://localhost:8080/api/inventories
Header: Authorization: Bearer YOUR_TOKEN
Body: {
  "branchId": null,
  "storeId": 6,
  "productId": 207,
  "quantity": 50,
  "unitPrice": 300
}
```

Expected response:
```json
{
  "id": 1,
  "branchId": null,
  "storeId": 6,
  "productId": 207,
  "productName": "Palpasa Café – Narayan Wagle",
  "quantity": 50,
  "unitPrice": 300
}
```

**Test 2: Get Inventory**
```
GET http://localhost:8080/api/inventories/store/6
Header: Authorization: Bearer YOUR_TOKEN
```

Expected response:
```json
[
  {
    "id": 1,
    "branchId": null,
    "storeId": 6,
    "productId": 207,
    "productName": "Palpasa Café – Narayan Wagle",
    "quantity": 50,
    "unitPrice": 300
  }
]
```

### Step 4: Check Frontend Redux State

In browser DevTools console:
```javascript
// Open Redux DevTools or check Redux state
// Look for the inventory slice
store.getState().inventory

// Should show:
{
  inventory: [
    {
      id: 1,
      branchId: null,
      storeId: 6,
      productId: 207,
      productName: "Palpasa Café",
      quantity: 50,
      unitPrice: 300
    }
  ],
  loading: false,
  error: null
}
```

---

## Common Issues & Fixes

### Issue 1: Inventory saved but API returns empty
**Cause:** Repository query issue
**Fix:** Verify the query in InventoryRepository:
```java
@Query("SELECT i FROM Inventory i WHERE i.store.id = :storeId OR (i.branch IS NOT NULL AND i.branch.store.id = :storeId)")
List<Inventory> findByStoreId(@Param("storeId") Long storeId);
```

### Issue 2: Database shows 0 quantity
**Cause:** Quantity not being set in entity
**Fix:** Check createInventory method is setting:
```java
.quantity(inventoryDTO.getQuantity())
```

### Issue 3: Frontend shows items but quantity is 0
**Cause:** Mapper not converting correctly
**Fix:** Verify InventoryMapper.toDTO() is converting quantity:
```java
.quantity(inventory.getQuantity())
```

### Issue 4: API returns empty array []
**Cause:** Query not matching any records
**Fix:** 
1. Check if store_id and branch_id values match database
2. Ensure branch_id IS NULL comparison works
3. Test query directly in database

---

## Quick Validation Checklist

- [ ] Backend logs show "Creating WAREHOUSE inventory" 
- [ ] Backend logs show "After save - Quantity: 50"
- [ ] Database query returns 1+ rows with quantity > 0
- [ ] POST /api/inventories returns quantity in response
- [ ] GET /api/inventories/store/6 returns array with items
- [ ] Frontend console shows "Found X inventory items"
- [ ] Redux state shows inventory array with items
- [ ] Frontend displays items in warehouse inventory table

---

## If Still Showing 0

Run this step-by-step:

1. **Check backend console** after adding:
   ```
   Is "=== CREATING INVENTORY ===" showing?
   Is "After save - Quantity" showing the right number?
   ```

2. **Check database**:
   ```sql
   SELECT COUNT(*) FROM inventory WHERE store_id = 6;
   SELECT * FROM inventory WHERE store_id = 6;
   ```

3. **Check API response**:
   Open DevTools Network tab
   Look for GET /api/inventories/store/6
   Check Response tab - is it returning []?

4. **Share the following in console**:
   - Backend console logs (copy all output when adding inventory)
   - Network tab response for GET /api/inventories/store/6
   - Frontend console logs from StoreWarehouseInventory component
   - Database query result from: `SELECT * FROM inventory WHERE store_id = 6;`

---

## Next Steps

After collecting the above information, we can pinpoint exactly where the issue is:
- **Issue in Service?** → Fix createInventory or getAllInventoryByStoreId
- **Issue in Database?** → Fix entity or migration
- **Issue in Mapper?** → Fix InventoryMapper.toDTO()
- **Issue in Repository?** → Fix query in InventoryRepository
- **Issue in Frontend?** → Fix Redux slice or component filtering
