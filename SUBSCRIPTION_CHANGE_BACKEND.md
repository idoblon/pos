# Subscription Change Request Backend Implementation

## Overview
Complete backend support for subscription plan changes (upgrades and downgrades) with payment verification and admin approval workflow.

## Database Table

### subscription_change_request
```sql
CREATE TABLE subscription_change_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    store_name VARCHAR(255),
    owner_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    current_plan VARCHAR(50) NOT NULL,
    requested_plan VARCHAR(50) NOT NULL,
    change_type VARCHAR(20) NOT NULL,  -- 'UPGRADE' or 'DOWNGRADE'
    amount DOUBLE NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_reference VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'PAID',  -- 'PAID', 'APPROVED', 'REJECTED'
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    approved_at TIMESTAMP,
    rejected_at TIMESTAMP,
    approved_by BIGINT,
    rejection_reason TEXT
);
```

## API Endpoints

### Store Admin Endpoints

#### Create Subscription Change Request
**POST** `/api/subscription-upgrade-requests`
- **Auth**: `ROLE_STORE_ADMIN`
- **Body**:
```json
{
  "storeId": 1,
  "storeName": "Indoor Plant World",
  "ownerName": "John Doe",
  "email": "john@example.com",
  "phone": "1234567890",
  "currentPlan": "BASIC",
  "requestedPlan": "PROFESSIONAL",
  "changeType": "UPGRADE",
  "amount": 3500,
  "paymentMethod": "ESEWA",
  "paymentReference": "ABC123456",
  "status": "PAID",
  "paidAt": "2024-01-15T10:30:00"
}
```
- **Response**: Created SubscriptionChangeRequest object

#### Get Store's Change Requests
**GET** `/api/subscription-upgrade-requests/store/{storeId}`
- **Auth**: `ROLE_STORE_ADMIN`
- **Response**: List of SubscriptionChangeRequest objects for the store

### POS Admin Endpoints

#### Get All Change Requests
**GET** `/api/admin/subscription-change-requests?status=PAID`
- **Auth**: `ROLE_ADMIN`
- **Query Params**: 
  - `status` (optional): Filter by status (PAID, APPROVED, REJECTED)
- **Response**: List of all SubscriptionChangeRequest objects

#### Approve Change Request
**POST** `/api/admin/subscription-change-requests/{id}/approve`
- **Auth**: `ROLE_ADMIN`
- **Response**: Success message
- **Side Effect**: Updates store's subscriptionPlan field automatically

#### Reject Change Request
**POST** `/api/admin/subscription-change-requests/{id}/reject`
- **Auth**: `ROLE_ADMIN`
- **Body**:
```json
{
  "reason": "Invalid payment reference"
}
```
- **Response**: Success message

## Files Created

### Entity
- `SubscriptionChangeRequest.java` - JPA entity with all request fields

### Repository
- `SubscriptionChangeRequestRepository.java` - Data access layer with query methods

### Service
- `SubscriptionChangeRequestService.java` - Service interface
- `SubscriptionChangeRequestServiceImpl.java` - Business logic implementation

### Controller
- `SubscriptionChangeRequestController.java` - REST endpoints for both store-admin and POS admin

## Features

### Support for Both Upgrades and Downgrades
- `changeType` field stores "UPGRADE" or "DOWNGRADE"
- Amount calculation handles price differences
- Frontend can request any plan change

### Automatic Store Update
- When admin approves request, store's `subscriptionPlan` field updates automatically
- Uses existing `StoreService.updateSubscriptionPlan()` method

### Payment Verification
- Stores payment method (ESEWA, KHALTI)
- Stores payment reference for verification
- Tracks payment timestamp

### Admin Workflow
- Admins can view all pending requests
- Filter by status (PAID, APPROVED, REJECTED)
- Approve with automatic plan activation
- Reject with reason stored

## Status Flow

1. **PAID** - Store admin submits request with payment reference (initial state)
2. **APPROVED** - POS admin approves → store's plan updated automatically
3. **REJECTED** - POS admin rejects with reason

## Integration with Existing Code

### Uses Existing Services
- `StoreService.updateSubscriptionPlan()` for plan updates
- `UserService.getUserFromJwtToken()` for authentication

### Compatible with Frontend
- Matches frontend API calls in `SubscriptionRequest.jsx`
- Stores data in same format expected by frontend
- Returns proper response structures

## Testing Steps

1. **Store Admin Creates Request**
```bash
POST /api/subscription-upgrade-requests
Authorization: Bearer <store-admin-jwt>
{
  "storeId": 1,
  "currentPlan": "BASIC",
  "requestedPlan": "ENTERPRISE",
  "changeType": "UPGRADE",
  "amount": 6500,
  "paymentMethod": "ESEWA",
  "paymentReference": "TEST123"
}
```

2. **POS Admin Views Requests**
```bash
GET /api/admin/subscription-change-requests?status=PAID
Authorization: Bearer <admin-jwt>
```

3. **POS Admin Approves**
```bash
POST /api/admin/subscription-change-requests/1/approve
Authorization: Bearer <admin-jwt>
```

4. **Verify Store Plan Updated**
```bash
GET /api/stores/admin
Authorization: Bearer <store-admin-jwt>
# Check subscriptionPlan field
```

## Security

- All endpoints require authentication
- Store admin can only create/view their own store's requests
- Only POS admin can approve/reject requests
- Uses `@PreAuthorize` for role-based access control

## Logging

All operations logged with:
- User ID (store admin or POS admin)
- Store ID
- Change type (upgrade/downgrade)
- Old and new plans
- Approval/rejection reasons
