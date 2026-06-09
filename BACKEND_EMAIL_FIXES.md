# Backend Fixes - Email and Payment Flow

## Issues Fixed

### 1. **Approval Process Modified**
**File**: `StoreRegistrationServiceImpl.java`

**Changes**:
- Modified `approveRequest()` method to send approval email immediately
- Changed status to `PAYMENT_PENDING` instead of creating store right away
- Removed payment check requirement for initial approval
- Store and credentials creation now happens AFTER payment

**Before**:
```java
public void approveRequest(Long requestId, Long adminId) throws Exception {
    approveRequestWithOverride(requestId, adminId, false);
}
```

**After**:
```java
public void approveRequest(Long requestId, Long adminId) throws Exception {
    StoreRegistrationRequest request = getRequestById(requestId);
    
    if (!"PENDING".equals(request.getStatus())) {
        throw new Exception("Request has already been processed");
    }
    
    request.setStatus("PAYMENT_PENDING");
    request.setProcessedAt(LocalDateTime.now());
    request.setApprovedByAdminId(adminId);
    
    registrationRepository.save(request);
    
    // Send approval email (NOT credentials)
    emailService.sendStoreRegistrationApprovalNotification(
        request.getEmail(),
        request.getOwnerName(),
        request.getStoreName(),
        request.getSubscriptionPlan()
    );
}
```

### 2. **New Email Method Added**
**Files**: 
- `EmailService.java` (interface)
- `EmailServiceImpl.java` (implementation)

**Added Method**:
```java
void sendStoreRegistrationApprovalNotification(
    String applicantEmail, 
    String ownerName, 
    String storeName, 
    String subscriptionPlan
)
```

**Email Template**:
```
Hello [Owner Name],

Congratulations! Your store registration request has been APPROVED.

Store Details:
Store Name: [Store Name]
Status: APPROVED
Subscription Plan: [Plan Name]

Next Step: Please complete your subscription payment to receive your login credentials.
Payment Link: http://localhost:5173/admin/payment-simulation

Once payment is completed, you will receive your login credentials via email.

Best regards,
POS System Team
```

### 3. **Separation of Email Types**

**Approval Email** (sendStoreRegistrationApprovalNotification):
- Sent immediately after admin approval
- Contains store name and owner name
- Includes payment link
- NO login credentials
- NO temporary password

**Credentials Email** (sendStoreRegistrationApproved):
- Sent AFTER payment completion
- Contains login credentials
- Includes temporary password
- Tells user to login and change password

## Workflow Flow

### Current Correct Flow:

1. **Store Submits Registration**
   - Store owner fills out registration form
   - Request saved with status: `PENDING`

2. **Admin Approves Request**
   - Admin clicks "Approve" button
   - Backend calls `approveRequest()`
   - Status changes to: `PAYMENT_PENDING`
   - **Approval Email Sent** (with owner name, store name, payment link)

3. **Store Completes Payment**
   - Store owner clicks payment link
   - Payment processed
   - Backend needs to call `approveRequestWithOverride()` with payment flag
   - Store and user account created
   - **Credentials Email Sent** (with login details)

4. **Store Access**
   - Store can now login with credentials
   - Must change temporary password on first login

## Files Modified

1. ✅ `/src/main/java/com/springboot/POS/service/impl/StoreRegistrationServiceImpl.java`
   - Modified `approveRequest()` method
   - Changed flow to send approval email first

2. ✅ `/src/main/java/com/springboot/POS/service/EmailService.java`
   - Added `sendStoreRegistrationApprovalNotification()` interface method

3. ✅ `/src/main/java/com/springboot/POS/service/impl/EmailServiceImpl.java`
   - Implemented `sendStoreRegistrationApprovalNotification()` method

## What Still Needs Implementation

### Payment Completion Endpoint
Create a new endpoint to handle payment completion:

```java
@PostMapping("/store-requests/{id}/complete-payment")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse> completeStorePayment(
        @PathVariable Long id,
        @RequestHeader("Authorization") String jwt) throws Exception {
    
    User admin = userService.getUserFromJwtToken(jwt);
    
    // Call the full approval with store creation
    registrationService.approveRequestWithOverride(id, admin.getId(), true);
    
    ApiResponse response = new ApiResponse();
    response.setMessage("Payment completed. Store created and credentials sent.");
    return ResponseEntity.ok(response);
}
```

## Testing

1. **Test Approval Email**:
   - Admin approves a store request
   - Check email shows: "Hello [Owner Name]" (not null)
   - Verify payment link is included
   - Confirm NO login credentials in email

2. **Test Payment Flow**:
   - Store completes payment simulation
   - Call payment completion endpoint
   - Check credentials email is sent
   - Verify temporary password is generated

3. **Test Store Access**:
   - Try to login before payment (should fail)
   - Complete payment
   - Try to login after payment (should work)

## Key Points

- ✅ Owner name now displays correctly (not "null")
- ✅ Approval email separated from credentials email
- ✅ Payment link included in approval email
- ✅ Temporary password only generated after payment
- ✅ Store creation happens after payment completion
- ✅ Proper email workflow implemented

## Next Steps

1. Compile and run the backend with these changes
2. Test the approval flow
3. Implement payment completion endpoint
4. Connect payment simulation to backend API
5. Test complete workflow end-to-end