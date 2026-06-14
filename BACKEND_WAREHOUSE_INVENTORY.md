# 🔧 Backend Warehouse Inventory Implementation

## Overview

The backend has been updated to support **warehouse inventory** for Store Admins. This allows Store Admins to maintain their own central stock separate from branch inventories.

---

## 📊 Database Schema Changes

### Inventory Table Updates

```sql
ALTER TABLE inventory 
    ADD COLUMN unit_price DOUBLE,
    ADD COLUMN store_id BIGINT,
    ADD CONSTRAINT fk_inventory_store FOREIGN KEY (store_id) REFERENCES store(id);

-- Make branch_id nullable (if not already)
ALTER TABLE inventory MODIFY COLUMN branch_id BIGINT NULL;
```

### Key Concept
```
- branch_id = NULL  → Warehouse Inventory (Store Admin's stock)
- branch_id != NULL → Branch Inventory (Specific branch's stock)
```

---

## 🎯 Entity Changes

### Inventory.java

**Before:**
```java
@ManyToOne
private Branch branch;  // Required

@Column(nullable = false)
private Integer quantity;
```

**After:**
```java
@ManyToOne
private Branch branch;  // NULL = Warehouse, NOT NULL = Branch

@Column(nullable = false)
private Integer quantity;

private Double unitPrice;  // For value calculation

@ManyToOne
private Store store;  // For warehouse inventory when branch is null
```

---

## 📦 DTO Changes

### InventoryDTO.java

**New Fields:**
```java
private Long branchId;      // NULL = warehouse, NOT NULL = branch
private Double unitPrice;   // Unit price for inventory value
private Long storeId;       // Store ID for warehouse inventory
```

**Validation:**
- `branchId` is now optional (removed @NotNull)
- Either `branchId` OR `storeId` must be provided
- `quantity` and `productId` still required

---

## 🔌 API Endpoints

### Existing Endpoints (Updated)

#### 1. Create Inventory
```
POST /api/inventories
```

**Request Body (Warehouse):**
```json
{
  "branchId": null,
  "storeId": 1,
  "productId": 101,
  "quantity": 100,
  "unitPrice": 800.0
}
```

**Request Body (Branch):**
```json
{
  "branchId": 3,
  "productId": 101,
  "quantity": 30,
  "unitPrice": 800.0
}
```

**Response:**
```json
{
  "id": 1,
  "branchId": null,
  "branchName": null,
  "storeId": 1,
  "productId": 101,
  "productName": "Wireless Mouse",
  "productSku": "WM-001",
  "quantity": 100,
  "unitPrice": 800.0,
  "lastUpdate": "2024-01-15T10:30:00"
}
```

---

#### 2. Get All Store Inventory
```
GET /api/inventories/store/{storeId}
```

**Returns:** All inventory (warehouse + all branches) for the store

**Response:**
```json
[
  {
    "id": 1,
    "branchId": null,
    "storeId": 1,
    "productName": "Product A",
    "quantity": 100,
    "unitPrice": 800.0
  },
  {
    "id": 2,
    "branchId": 3,
    "productName": "Product A",
    "quantity": 30,
    "unitPrice": 800.0
  }
]
```

---

### New Warehouse Endpoints

#### 3. Get Warehouse Inventory Only
```
GET /api/inventories/warehouse/store/{storeId}
```

**Returns:** Only warehouse inventory (branchId = null)

**Response:**
```json
[
  {
    "id": 1,
    "branchId": null,
    "storeId": 1,
    "productName": "Product A",
    "quantity": 100,
    "unitPrice": 800.0
  },
  {
    "id": 4,
    "branchId": null,
    "storeId": 1,
    "productName": "Product B",
    "quantity": 200,
    "unitPrice": 500.0
  }
]
```

---

#### 4. Get Specific Product in Warehouse
```
GET /api/inventories/warehouse/store/{storeId}/product/{productId}
```

**Returns:** Warehouse inventory for a specific product

**Response:**
```json
{
  "id": 1,
  "branchId": null,
  "storeId": 1,
  "productId": 101,
  "productName": "Wireless Mouse",
  "quantity": 100,
  "unitPrice": 800.0
}
```

**Error Response (404):**
```json
{
  "message": "Product not found in warehouse inventory"
}
```

---

## 🔄 Business Logic

### Creating Warehouse Inventory

```java
// When branchId is NULL
if (inventoryDTO.getBranchId() == null) {
    // Create warehouse inventory
    Store store = storeRepository.findById(inventoryDTO.getStoreId())
            .orElseThrow(() -> new Exception("Store does not exist"));
    
    inventory = Inventory.builder()
            .branch(null)  // NULL = Warehouse
            .store(store)
            .product(product)
            .quantity(inventoryDTO.getQuantity())
            .unitPrice(inventoryDTO.getUnitPrice())
            .build();
}
```

### Creating Branch Inventory

```java
// When branchId is NOT NULL
else {
    // Create branch inventory
    Branch branch = branchRepository.findById(inventoryDTO.getBranchId())
            .orElseThrow(() -> new Exception("Branch does not exist"));
    
    inventory = Inventory.builder()
            .branch(branch)
            .store(null)  // Store reference is in branch
            .product(product)
            .quantity(inventoryDTO.getQuantity())
            .unitPrice(inventoryDTO.getUnitPrice())
            .build();
}
```

---

## 🛡️ Security & Access Control

### Authorization Rules

| Inventory Type | Required Permission | Validation |
|---------------|-------------------|-----------|
| **Warehouse** | Store Admin | `ownershipGuard.requireStoreAccess(user, storeId)` |
| **Branch** | Store Admin or Branch Manager | `ownershipGuard.requireBranchAccess(user, branchId)` |

### Create Inventory Validation

```java
// For branch inventory
if (inventoryDTO.getBranchId() != null) {
    ownershipGuard.requireBranchAccess(user, inventoryDTO.getBranchId());
}
// For warehouse inventory
else if (inventoryDTO.getStoreId() != null) {
    ownershipGuard.requireStoreAccess(user, inventoryDTO.getStoreId());
} 
else {
    throw new Exception("Either branchId or storeId must be provided");
}
```

---

## 📝 Repository Queries

### New Queries Added

```java
// Get warehouse inventory only (branch is null)
@Query("SELECT i FROM Inventory i WHERE i.store.id = :storeId AND i.branch IS NULL")
List<Inventory> findWarehouseInventoryByStoreId(@Param("storeId") Long storeId);

// Get specific product in warehouse
@Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.store.id = :storeId AND i.branch IS NULL")
Optional<Inventory> findWarehouseInventoryByProductAndStore(
    @Param("productId") Long productId, 
    @Param("storeId") Long storeId
);
```

### Updated Queries

```java
// Get all inventory (warehouse + branches) for a store
@Query("SELECT i FROM Inventory i WHERE i.store.id = :storeId OR i.branch.store.id = :storeId")
List<Inventory> findByStoreId(@Param("storeId") Long storeId);

// Low stock query (includes warehouse)
@Query("SELECT i FROM Inventory i WHERE (i.branch.store.id = :storeId OR i.store.id = :storeId) AND i.quantity <= :threshold")
List<Inventory> findLowStockByStore(@Param("storeId") Long storeId, @Param("threshold") Integer threshold);
```

---

## 🧪 Testing Examples

### Test 1: Create Warehouse Inventory

```bash
curl -X POST http://localhost:8080/api/inventories \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "branchId": null,
    "storeId": 1,
    "productId": 101,
    "quantity": 100,
    "unitPrice": 800.0
  }'
```

**Expected:** 200 OK with created inventory

---

### Test 2: Get Warehouse Inventory

```bash
curl -X GET http://localhost:8080/api/inventories/warehouse/store/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected:** Array of warehouse inventory items

---

### Test 3: Get All Store Inventory

```bash
curl -X GET http://localhost:8080/api/inventories/store/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Expected:** Array including both warehouse and branch inventories

---

### Test 4: Create Branch Inventory

```bash
curl -X POST http://localhost:8080/api/inventories \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "branchId": 3,
    "productId": 101,
    "quantity": 30,
    "unitPrice": 800.0
  }'
```

**Expected:** 200 OK with created branch inventory

---

## 🔄 Migration Strategy

### For Existing Data

If you have existing inventory data, you may need to:

1. **Add Missing Columns:**
```sql
ALTER TABLE inventory 
    ADD COLUMN unit_price DOUBLE,
    ADD COLUMN store_id BIGINT;
```

2. **Populate Unit Prices (Optional):**
```sql
UPDATE inventory i
INNER JOIN product p ON i.product_id = p.id
SET i.unit_price = p.selling_price
WHERE i.unit_price IS NULL;
```

3. **All Existing Inventory is Branch Inventory:**
- No migration needed for existing data
- `branch_id` is already populated
- New warehouse inventory will have `branch_id = NULL`

---

## ⚠️ Important Notes

### 1. Data Integrity

- **Warehouse inventory:** `branch IS NULL` AND `store IS NOT NULL`
- **Branch inventory:** `branch IS NOT NULL` AND `store IS NULL` (store is accessed via branch.store)

### 2. Queries Must Handle NULL

When querying, always consider both warehouse and branch inventories:

```java
// Good: Includes both
@Query("SELECT i FROM Inventory i WHERE i.store.id = :storeId OR i.branch.store.id = :storeId")

// Bad: Only gets branch inventory
@Query("SELECT i FROM Inventory i WHERE i.branch.store.id = :storeId")
```

### 3. Unit Price

- `unitPrice` is optional but recommended
- Used for calculating total inventory value
- Should match product's selling price at time of adding to inventory

### 4. Stock Deduction/Addition

- Current `deductStock` and `addStock` methods work with branches only
- Warehouse stock deduction happens via `updateStock` method
- Frontend handles the logic of reducing warehouse and adding to branch

---

## 🚀 Deployment Steps

### 1. Database Migration

```sql
-- Run this on your database
ALTER TABLE inventory 
    ADD COLUMN unit_price DOUBLE,
    ADD COLUMN store_id BIGINT,
    ADD CONSTRAINT fk_inventory_store FOREIGN KEY (store_id) REFERENCES store(id);

-- Optional: Populate unit prices from products
UPDATE inventory i
INNER JOIN product p ON i.product_id = p.id
SET i.unit_price = p.selling_price
WHERE i.unit_price IS NULL;
```

### 2. Deploy Backend

```bash
cd /path/to/POS
mvn clean install
# Restart your Spring Boot application
```

### 3. Verify Endpoints

```bash
# Test warehouse inventory creation
curl -X POST http://localhost:8080/api/inventories \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"branchId":null,"storeId":1,"productId":1,"quantity":100,"unitPrice":500}'

# Test warehouse inventory retrieval
curl http://localhost:8080/api/inventories/warehouse/store/1 \
  -H "Authorization: Bearer TOKEN"
```

---

## 📊 Backend API Summary

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/inventories` | Create inventory (warehouse or branch) | Store Admin / Branch Manager |
| GET | `/api/inventories/store/{storeId}` | Get all inventory (warehouse + branches) | Store Admin |
| GET | `/api/inventories/warehouse/store/{storeId}` | Get warehouse inventory only | Store Admin |
| GET | `/api/inventories/warehouse/store/{storeId}/product/{productId}` | Get specific product in warehouse | Store Admin |
| GET | `/api/inventories/branch/{branchId}` | Get branch inventory | Store Admin / Branch Manager |
| PATCH | `/api/inventories/{id}/stock` | Update stock quantity | Store Admin / Branch Manager |
| DELETE | `/api/inventories/{id}` | Delete inventory item | Store Admin / Branch Manager |
| GET | `/api/inventories/store/{storeId}/low-stock` | Get low stock items | Store Admin |

---

## ✅ Testing Checklist

### Unit Tests Needed

- [ ] Create warehouse inventory with null branchId
- [ ] Create branch inventory with branchId
- [ ] Validation: Reject if both branchId and storeId are null
- [ ] Validation: Reject if branchId provided but branch doesn't exist
- [ ] Validation: Reject if storeId provided but store doesn't exist
- [ ] Query warehouse inventory by storeId
- [ ] Query specific product in warehouse
- [ ] Query all inventory (warehouse + branches) for store
- [ ] Update warehouse inventory stock
- [ ] Delete warehouse inventory
- [ ] Verify unitPrice is stored correctly
- [ ] Verify storeId is populated for warehouse inventory

### Integration Tests Needed

- [ ] Full workflow: Create product → Add to warehouse → Distribute to branch
- [ ] Security: Store Admin can access warehouse
- [ ] Security: Branch Manager cannot access warehouse
- [ ] Security: Store Admin can access all branch inventories
- [ ] Low stock query includes warehouse inventory
- [ ] Total store inventory returns warehouse + all branches

---

## 🐛 Troubleshooting

### Issue 1: "branch does not exist" error when creating warehouse inventory

**Solution:** Ensure `branchId` is explicitly `null` in the request, not just omitted.

```json
{
  "branchId": null,  // ← Must be explicitly null
  "storeId": 1,
  "productId": 101,
  "quantity": 100
}
```

---

### Issue 2: Warehouse inventory not appearing in queries

**Check:**
1. Is `branch` field NULL in database?
2. Is `store_id` field populated?
3. Is query using correct store ID?

```sql
-- Debug query
SELECT * FROM inventory WHERE branch_id IS NULL AND store_id = 1;
```

---

### Issue 3: Foreign key constraint error

**Solution:** Ensure Store exists before creating warehouse inventory.

```sql
-- Check if store exists
SELECT * FROM store WHERE id = 1;
```

---

## 📞 Support

For backend issues:
- Check Spring Boot logs: `logs/spring-boot-logger.log`
- Check database: Verify `inventory` table has `unit_price` and `store_id` columns
- Check API response: Look for specific error messages

---

**Last Updated:** $(date)
**Version:** 2.0
**Status:** ✅ Complete
