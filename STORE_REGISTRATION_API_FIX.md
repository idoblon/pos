# Backend API Implementation - Store Registration Requests

## Problem
The frontend was making API calls to endpoints that didn't exist on the backend, causing 404 errors:
- `GET /api/admin/store-requests?status=PENDING`
- `GET /api/admin/store-requests/pending/count`
- `POST /api/admin/store-requests/{id}/approve`
- `POST /api/admin/store-requests/{id}/reject`

## Solution
Created a new `AdminController` that provides the exact API endpoints expected by the frontend.

## Backend Changes

### New File Created
**Location:** `src/main/java/com/springboot/POS/controller/AdminController.java`

### API Endpoints Implemented

#### 1. Get Store Registration Requests (with optional status filter)
```
GET /api/admin/store-requests?status=PENDING
```
- Returns list of store registration requests
- Optional `status` query parameter filters by status (PENDING, APPROVED, REJECTED)
- Requires ADMIN role

#### 2. Get Pending Requests Count
```
GET /api/admin/store-requests/pending/count
```
- Returns the count of pending registration requests
- Used for notification badge in admin dashboard
- Requires ADMIN role

#### 3. Approve Store Registration Request
```
POST /api/admin/store-requests/{id}/approve
```
- Approves a registration request
- Creates store and store admin user
- Sends approval email with credentials
- Requires ADMIN role

#### 4. Reject Store Registration Request
```
POST /api/admin/store-requests/{id}/reject
Body: { "reason": "Rejection reason here" }
```
- Rejects a registration request
- Sends rejection email with reason
- Requires ADMIN role

## Frontend Changes

### Updated Files
1. **StoreRegistrationRequests.jsx** - Restored real API calls
2. **AdminLayout.jsx** - Restored real API calls for pending count
3. **Deleted mockStoreApi.js** - Removed temporary mock API

## How It Works

1. **Frontend makes request** → `GET /api/admin/store-requests?status=PENDING`
2. **Security filter validates JWT** → Checks if user has ADMIN role
3. **AdminController receives request** → Calls StoreRegistrationService
4. **Service queries database** → Returns filtered results
5. **Response sent to frontend** → Displays in UI

## Security
- All endpoints require JWT authentication
- All endpoints require ADMIN role via `@PreAuthorize("hasRole('ADMIN')")`
- CORS configured for `http://localhost:5173`

## Testing Steps

1. **Start Backend:**
   ```bash
   cd c:\Users\hp\IdeaProjects\POS
   mvn spring-boot:run
   ```

2. **Start Frontend:**
   ```bash
   cd c:\Users\hp\IdeaProjects\pos-frontend
   npm run dev
   ```

3. **Login as Admin** and navigate to Registration Requests page

4. **Expected Results:**
   - ✅ No 404 errors in console
   - ✅ Registration requests load from database
   - ✅ Notification bell shows pending count
   - ✅ Approve/Reject functionality works
   - ✅ Email notifications sent

## Database Requirements

Ensure the `store_registration_request` table exists in your MySQL database. The table will be auto-created by Hibernate if `spring.jpa.hibernate.ddl-auto=update` is set.

## Additional Notes

- The backend already had the service layer implemented in `StoreRegistrationServiceImpl`
- The new `AdminController` simply provides the frontend-expected endpoints that route to existing services
- The original `RegistrationRequestController` at `/api/admin/registration-requests` still exists and can be used alternatively
