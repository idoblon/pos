# Subscription Upgrade Implementation

## Overview
Implemented complete subscription upgrade workflow allowing store-admins to request plan changes and POS admins to approve them, with automatic database updates.

## Backend Changes

### 1. AdminController.java
**File**: `src/main/java/com/springboot/POS/controller/AdminController.java`

**Added Endpoint**:
```java
@PutMapping("/stores/{storeId}/subscription")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse> updateStoreSubscription(
    @PathVariable Long storeId,
    @RequestBody Map<String, String> request,
    @RequestHeader("Authorization") String jwt
)
```

**Purpose**: Allows POS admin to update a store's subscription plan in the database after approving an upgrade request.

**Request Body**:
```json
{
  "subscriptionPlan": "ENTERPRISE"
}
```

### 2. StoreService.java
**File**: `src/main/java/com/springboot/POS/service/StoreService.java`

**Added Method**:
```java
void updateSubscriptionPlan(Long storeId, String subscriptionPlan) throws Exception;
```

### 3. StoreServiceImpl.java
**File**: `src/main/java/com/springboot/POS/service/impl/StoreServiceImpl.java`

**Implementation**:
```java
@Override
public void updateSubscriptionPlan(Long storeId, String subscriptionPlan) throws Exception {
    Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new Exception("Store not found with id: " + storeId));
    
    store.setSubscriptionPlan(subscriptionPlan);
    storeRepository.save(store);
}
```

**Purpose**: Updates the `subscription_plan` field in the Store entity.

## Frontend Changes

### 1. SubscriptionManagement.jsx (POS Admin)
**File**: `src/pages/admin/Subscriptions/SubscriptionManagement.jsx`

**Updated**: `approveUpgrade` function to call backend API
```javascript
// Step 1: Approve the upgrade request
await api.post(`/api/admin/subscription-upgrade-requests/${request.id}/approve`, {}, { headers });

// Step 2: Update the store's subscription plan in database
await api.put(`/api/admin/stores/${request.storeId}/subscription`, 
  { subscriptionPlan: request.requestedPlan },
  { headers }
);

// Step 3: Refresh store list to show updated plan
await dispatch(getAllStores());
```

### 2. SubscriptionRequest.jsx (Store Admin)
**File**: `src/pages/storeAdmin/Subscription/SubscriptionRequest.jsx`

**Updated**: `refresh` function to properly fetch updated data
```javascript
const refresh = async () => {
  await dispatch(getStoreByAdmin());
  const updatedRequests = readRequests().map(normalizeRequest);
  setRequests(updatedRequests);
  
  // Auto-hide payment form if request was approved
  const activeReq = updatedRequests.find(
    (request) => request.storeId === storeId && 
    !["APPROVED", "REJECTED"].includes(request.status)
  );
  if (!activeReq) {
    setShowPaymentForm(false);
  }
};
```

## Database Schema

### Store Table
The `Store` entity already has the required field:

```java
@Column(name = "subscription_plan")
private String subscriptionPlan; // BASIC, PROFESSIONAL, ENTERPRISE
```

## Workflow

1. **Store-Admin Requests Upgrade**:
   - Navigates to Subscription page
   - Clicks "Upgrade Plan"
   - Fills payment form with plan and payment reference
   - Submits request → saved to localStorage and backend

2. **POS Admin Approves**:
   - Views request in Subscription Management page
   - Clicks "Mark Paid" (if needed)
   - Clicks "Approve Upgrade"
   - Backend updates:
     - Request status → APPROVED
     - Store's subscriptionPlan field → new plan (e.g., ENTERPRISE)

3. **Store-Admin Sees Update**:
   - Clicks "Refresh" on Subscription page
   - Backend returns updated store data with new subscriptionPlan
   - Page displays: "Active Plan: Enterprise"

## API Endpoints

### POS Admin APIs
- `GET /api/admin/subscription-upgrade-requests` - Get all upgrade requests
- `POST /api/admin/subscription-upgrade-requests/{id}/mark-paid` - Mark request as paid
- `POST /api/admin/subscription-upgrade-requests/{id}/approve` - Approve request
- `PUT /api/admin/stores/{storeId}/subscription` - Update store's subscription plan ✨ **NEW**

### Store Admin APIs
- `GET /api/stores/admin` - Get store by authenticated admin (returns subscriptionPlan)
- `POST /api/subscription-upgrade-requests` - Submit upgrade request

## Subscription Plans

```javascript
const SUBSCRIPTION_PLANS = {
  BASIC: {
    price: 3500,
    features: ["3 branches", "10 users", "Basic support"]
  },
  PROFESSIONAL: {
    price: 7000,
    features: ["10 branches", "50 users", "Priority support", "Advanced reports"]
  },
  ENTERPRISE: {
    price: 10000,
    features: ["Unlimited branches", "Unlimited users", "24/7 support", "Custom features"]
  }
};
```

## Testing Steps

1. **Login as Store Admin** (e.g., Indoor Plant World)
2. Navigate to **Subscription** page
3. Current plan should show (e.g., Basic)
4. Click **"Upgrade Plan"** button
5. Select **Enterprise** plan
6. Enter payment reference (e.g., ESW123)
7. Click **"Send Paid Request to POS Admin"**

8. **Login as POS Admin**
9. Navigate to **Subscription Plans** page
10. Find Indoor Plant World store
11. Click **"Approve Upgrade"**
12. Confirm approval

13. **Return to Store Admin**
14. Click **"Refresh"** button on Subscription page
15. Verify: "Active Plan: Enterprise" is displayed ✅

## Notes

- Backend now properly persists subscription changes to database
- Store-admin sees real-time updates after approval
- POS admin uses localStorage overrides for immediate UI feedback
- Store data includes subscriptionPlan field in API responses
- No manual database updates required

## Files Modified

### Backend
- `AdminController.java` - Added subscription update endpoint
- `StoreService.java` - Added updateSubscriptionPlan interface method
- `StoreServiceImpl.java` - Implemented subscription plan update logic

### Frontend
- `SubscriptionManagement.jsx` - Enhanced approval flow with database update
- `SubscriptionRequest.jsx` - Improved refresh logic and form state management
- `registrationDataMerger.js` - Cleaned up debug logging

## Security

- All endpoints require authentication via JWT token
- `@PreAuthorize("hasRole('ADMIN')")` ensures only POS admin can update subscriptions
- Store admins can only view their own store data
- Input validation on subscriptionPlan field (converted to uppercase)
