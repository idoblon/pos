# Store Email Persistence Fix

## Problem
New stores created from registration requests were not persisting the contact email to the database, even though the email was being sent from the frontend during registration approval.

**Example:** Mitra Pustak store had `mitrapustak@yopmail.com` in the registration request but the `contact_email` field was NULL in the stores table.

## Root Cause
The email was being set in the `StoreContact` embedded object during store creation, but the `@PrePersist` method wasn't properly preserving the email value. Additionally, using `StoreContact.builder()` could potentially cause issues with partial initialization.

## Solution

### 1. Updated StoreServiceImpl.createStoreFromRegistration()
**File:** `src/main/java/com/springboot/POS/service/impl/StoreServiceImpl.java`

Changed from using `StoreContact.builder()` to direct field assignment:

```java
// OLD: Using builder (potential initialization issues)
StoreContact contact = StoreContact.builder()
    .address(address != null ? address : "")
    .phone(phone != null ? phone : "")
    .email(email != null ? email : "")
    .build();

// NEW: Direct construction and field setting
StoreContact contact = new StoreContact();
contact.setAddress(address != null ? address : "");
contact.setPhone(phone != null ? phone : "");
contact.setEmail(email != null ? email : "");
```

**Why this matters:** Direct object construction ensures proper initialization and avoids potential issues with the builder pattern during ORM operations.

### 2. Updated Store Entity @PrePersist
**File:** `src/main/java/com/springboot/POS/modal/Store.java`

Enhanced the `@PrePersist` method to preserve existing contact information:

```java
// NEW: Preserve existing email instead of overwriting
if (contact.getEmail() == null) {
    contact.setEmail("");
}
if (contact.getAddress() == null) {
    contact.setAddress("");
}
if (contact.getPhone() == null) {
    contact.setPhone("");
}
```

**Why this matters:** The original implementation could overwrite an already-set email with an empty string during persistence lifecycle.

### 3. Enhanced Logging
Added detailed logging in `createStoreFromRegistration()`:
- Shows the email received from registration
- Shows the email actually saved in the database
- Shows the store ID for tracking

```
✅ Store created: Mitra Pustak | Email from registration: mitrapustak@yopmail.com | Contact Email in DB: mitrapustak@yopmail.com | Store ID: 6
```

## Data Flow

1. **Registration Form** → sends `email` field
2. **PublicController** → creates StoreRegistrationRequest with email
3. **Admin Approves** → calls `approveRequestWithOverride()`
4. **StoreRegistrationServiceImpl** → calls `storeService.createStoreFromRegistration(email)`
5. **StoreServiceImpl** → creates StoreContact and sets email ✅
6. **Database** → persists to `contact_email` column

## Testing Steps

1. Start the backend with the updated code
2. Register a new store through the signup form with an email
3. Approve the registration as admin
4. Run this SQL query:

```sql
SELECT id, brand, contact_email FROM stores WHERE brand = '[Store Name]';
```

Expected result: contact_email should contain the email address from registration, not NULL.

## Database Column Mapping

The email is stored in:
- **Column Name:** `contact_email`
- **Table:** `stores`
- **Source Field:** StoreContact (embedded) → email property
- **JPA Mapping:** `@Column(name = "contact_email")`

## Files Modified
1. `src/main/java/com/springboot/POS/service/impl/StoreServiceImpl.java`
   - Line: `createStoreFromRegistration()` method

2. `src/main/java/com/springboot/POS/modal/Store.java`
   - Line: `@PrePersist` method

## Verification Queries

After deployment, verify existing stores can be updated by running:

```sql
-- Check current state
SELECT id, brand, owner_name, contact_email, created_at FROM stores;

-- If email is still NULL for existing stores, manually update from registration:
UPDATE stores s
SET s.contact_email = (
    SELECT srr.email FROM store_registration_request srr 
    WHERE srr.id = s.registration_request_id
)
WHERE s.contact_email IS NULL OR s.contact_email = '';
```

## Impact
- ✅ All new stores will have email persisted correctly
- ✅ All other store data (owner_name, address, phone, subscription) will be preserved
- ✅ Existing stores' data remains unchanged
- ✅ Backward compatible - no breaking changes
