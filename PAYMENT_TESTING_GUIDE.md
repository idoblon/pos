# Payment Integration Testing Guide

## Test Credentials

### eSewa Test Environment
- **Merchant ID**: `EPAYTEST`
- **Secret Key**: `8gBm/:&EnhH.1/q`
- **Service URL**: `https://uat.esewa.com.np/epay/main`
- **Test Mode**: Yes (UAT Environment)

### Khalti Test Environment
- **Public Key**: `test_public_key_dc74e0fd57cb46cd93832aee0a507256`
- **Secret Key**: `test_secret_key_f59e8b7d18b4499ca40f68195a846e9b`
- **Test Mode**: Yes

## Testing Flow

### 1. Store Registration with Payment

#### Step 1: Submit Registration Request
```bash
POST http://localhost:8080/api/public/store-registration-request
Content-Type: application/json

{
  "ownerName": "John Doe",
  "email": "john@example.com",
  "phone": "9876543210",
  "password": "password123",
  "storeName": "Test Store",
  "storeDescription": "A test store",
  "storeType": "RETAIL",
  "storeAddress": "123 Test Street",
  "subscriptionPlan": "BASIC"
}
```

**Response**: Registration created with status `PAYMENT_PENDING`

#### Step 2: Initiate Payment
```bash
POST http://localhost:8080/api/public/payments/initiate
Content-Type: application/json

{
  "registrationRequestId": 1,
  "paymentMethod": "ESEWA"
}
```

**Response**: Payment URL for redirection

#### Step 3: Test Payment Process

**For eSewa:**
1. Use the returned payment URL
2. You'll be redirected to eSewa test environment
3. Use these test credentials:
   - **Mobile/Email**: Any valid format
   - **MPIN**: `1234` (default test MPIN)
   - **OTP**: `123456` (test OTP)

**For Khalti:**
1. Use the returned Khalti configuration
2. Integrate with Khalti SDK in frontend
3. Test credentials:
   - **Mobile**: `9800000000`
   - **OTP**: `987654`

#### Step 4: Payment Verification
```bash
POST http://localhost:8080/api/public/payments/verify
Content-Type: application/json

{
  "transactionId": "TXN_123456789_abcd1234",
  "paymentGatewayReference": "esewa_ref_or_khalti_idx"
}
```

#### Step 5: Check Payment Status
```bash
GET http://localhost:8080/api/public/payments/status/1
```

#### Step 6: Admin Approval (After Payment)
```bash
POST http://localhost:8080/api/admin/registration-requests/1/approve
Authorization: Bearer {admin_jwt_token}
```

## Subscription Plans & Pricing

| Plan | Price (NPR) | Features |
|------|-------------|-----------|
| BASIC | 2,999/month | 1 Store, 3 Branches, 10 Users |
| PROFESSIONAL | 5,999/month | 1 Store, 10 Branches, 50 Users |
| ENTERPRISE | 12,999/month | Unlimited Stores, Branches, Users |

## Payment Status Flow

```
Registration → PAYMENT_PENDING → Payment Initiated → Payment Completed → PENDING → Admin Approval → APPROVED
```

## Test Scenarios

### Scenario 1: Successful eSewa Payment
1. Register store with BASIC plan
2. Initiate eSewa payment
3. Complete payment in test environment
4. Verify payment success
5. Admin approves request

### Scenario 2: Failed Payment
1. Register store
2. Initiate payment
3. Cancel payment or use invalid credentials
4. Check payment status shows "FAILED"
5. Admin cannot approve until payment is completed

### Scenario 3: Khalti Integration
1. Register store with PROFESSIONAL plan
2. Use Khalti payment method
3. Test with Khalti SDK integration
4. Verify payment through callback

## API Endpoints Summary

### Public Endpoints (No Authentication)
- `POST /api/public/store-registration-request` - Submit registration
- `POST /api/public/payments/initiate` - Start payment
- `POST /api/public/payments/verify` - Verify payment
- `GET /api/public/payments/status/{id}` - Check payment status
- `GET /api/public/payments/plans` - Get subscription plans
- `POST /api/public/payments/callback/esewa` - eSewa callback
- `POST /api/public/payments/callback/khalti` - Khalti callback

### Admin Endpoints (Require ADMIN role)
- `GET /api/admin/registration-requests` - Get all requests
- `GET /api/admin/registration-requests/pending` - Get pending requests
- `POST /api/admin/registration-requests/{id}/approve` - Approve (requires payment)
- `POST /api/admin/registration-requests/{id}/reject` - Reject request

## Important Notes

1. **Test Environment**: All credentials are for testing only
2. **Payment Validation**: Admin approval requires completed payment
3. **Status Tracking**: Multiple statuses track the entire flow
4. **Email Notifications**: Sent at registration, payment, and approval
5. **Security**: Payment verification prevents fraud

## Troubleshooting

### Common Issues:
1. **403 on Admin Endpoints**: Ensure user has ADMIN role
2. **Payment Not Found**: Check transaction ID format
3. **Approval Fails**: Verify payment is completed
4. **Gateway Timeout**: Use test credentials provided

### Debug Commands:
```sql
-- Check registration status
SELECT id, status, paymentStatus, subscriptionPlan, subscriptionAmount FROM store_registration_request;

-- Check payment details
SELECT * FROM subscription_payment WHERE registrationRequestId = 1;
```

## Frontend Integration Example

```javascript
// 1. Submit registration
const registration = await fetch('/api/public/store-registration-request', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(registrationData)
});

// 2. Initiate payment
const payment = await fetch('/api/public/payments/initiate', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    registrationRequestId: 1,
    paymentMethod: 'ESEWA'
  })
});

// 3. Redirect to payment URL
window.location.href = paymentResponse.paymentUrl;
```

This setup allows complete testing of the payment integration with both eSewa and Khalti using their test environments.