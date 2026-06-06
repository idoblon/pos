# Store Payment Methods Configuration Guide

## Overview

The POS system now supports multiple payment methods for stores and branches:
- **CASH** - Cash payments (enabled by default)
- **ESEWA** - eSewa digital wallet payments
- **KHALTI** - Khalti digital wallet payments
- **CARD** - Credit/Debit card payments
- **BANK_TRANSFER** - Direct bank transfers

## Automatic Setup

When a store registration is approved by admin:
1. Store and admin user are created
2. **All payment methods are automatically initialized**:
   - **CASH**: Enabled by default
   - **ESEWA, KHALTI, CARD, BANK_TRANSFER**: Disabled (requires configuration)

## Payment Configuration Workflow

### Step 1: Store Registration Approval
```
Admin approves registration → Store created → Payment methods initialized
```

### Step 2: Configure Payment Gateways
Store admin must configure payment gateway credentials:

#### Configure eSewa
```bash
POST /api/payment-config
Authorization: Bearer {store_admin_token}
Content-Type: application/json

{
  "paymentType": "ESEWA",
  "isEnabled": true,
  "esewaSettlementId": "EPAYTEST",
  "esewaSecretKey": "8gBm/:&EnhH.1/q",
  "notes": "eSewa test environment"
}
```

#### Configure Khalti
```bash
POST /api/payment-config
Authorization: Bearer {store_admin_token}
Content-Type: application/json

{
  "paymentType": "KHALTI",
  "isEnabled": true,
  "khaltiPublicKey": "test_public_key_dc74e0fd57cb46cd93832aee0a507256",
  "khaltiSecretKey": "test_secret_key_f59e8b7d18b4499ca40f68195a846e9b",
  "notes": "Khalti test environment"
}
```

#### Configure Card Payments
```bash
POST /api/payment-config
Authorization: Bearer {store_admin_token}
Content-Type: application/json

{
  "paymentType": "CARD",
  "isEnabled": true,
  "cardProcessorName": "Stripe",
  "cardApiKey": "pk_test_...",
  "cardSecretKey": "sk_test_...",
  "notes": "Stripe integration for card payments"
}
```

#### Configure Bank Transfer
```bash
POST /api/payment-config
Authorization: Bearer {store_admin_token}
Content-Type: application/json

{
  "paymentType": "BANK_TRANSFER",
  "isEnabled": true,
  "bankName": "Nepal Bank Limited",
  "accountNumber": "1234567890",
  "accountHolderName": "Store Name",
  "ifscCode": "NBL123456",
  "notes": "Bank transfer instructions for customers"
}
```

## API Endpoints

### Get All Payment Configurations
```bash
GET /api/payment-config/store
Authorization: Bearer {token}
```
Returns all payment methods configured for the store.

### Get Enabled Payment Methods
```bash
GET /api/payment-config/store/enabled
Authorization: Bearer {token}
```
Returns only enabled payment methods (available for transactions).

### Update Payment Configuration
```bash
PUT /api/payment-config/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "esewaSettlementId": "NEW_SETTLEMENT_ID",
  "isEnabled": true
}
```

### Enable/Disable Payment Method
```bash
PATCH /api/payment-config/{id}/toggle?isEnabled=true
Authorization: Bearer {token}
```

### Delete Payment Configuration
```bash
DELETE /api/payment-config/{id}
Authorization: Bearer {token}
```

### Re-initialize Default Payment Methods
```bash
POST /api/payment-config/initialize
Authorization: Bearer {token}
```
Useful if payment methods need to be reset.

## Order Payment Flow

### 1. Cashier Creates Order
When creating an order, cashier selects from available payment methods:

```javascript
// Frontend: Get enabled payment methods
const response = await fetch('/api/payment-config/store/enabled', {
  headers: { 'Authorization': 'Bearer ' + token }
});
const enabledMethods = await response.json();
// Display only enabled methods to cashier
```

### 2. Process Payment Based on Type

#### Cash Payment
```javascript
{
  "paymentType": "CASH",
  "totalAmount": 1500.00
}
// No additional processing needed
```

#### eSewa Payment
```javascript
{
  "paymentType": "ESEWA",
  "totalAmount": 1500.00,
  "esewaTransactionId": "esewa_ref_123"
}
// Verify with eSewa gateway
```

#### Khalti Payment
```javascript
{
  "paymentType": "KHALTI",
  "totalAmount": 1500.00,
  "khaltiToken": "khalti_token_abc"
}
// Verify with Khalti gateway
```

#### Card Payment
```javascript
{
  "paymentType": "CARD",
  "totalAmount": 1500.00,
  "cardToken": "tok_visa_123"
}
// Process through payment processor
```

#### Bank Transfer
```javascript
{
  "paymentType": "BANK_TRANSFER",
  "totalAmount": 1500.00,
  "bankReference": "bank_ref_789"
}
// Mark as pending, verify manually
```

## Database Schema

### store_payment_config Table
```sql
CREATE TABLE store_payment_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  payment_type VARCHAR(50) NOT NULL,
  is_enabled BOOLEAN DEFAULT TRUE,
  
  -- eSewa
  esewa_settlement_id VARCHAR(255),
  esewa_secret_key VARCHAR(255),
  
  -- Khalti
  khalti_public_key VARCHAR(255),
  khalti_secret_key VARCHAR(255),
  
  -- Card
  card_processor_name VARCHAR(100),
  card_api_key VARCHAR(255),
  card_secret_key VARCHAR(255),
  
  -- Bank
  bank_name VARCHAR(255),
  account_number VARCHAR(50),
  account_holder_name VARCHAR(255),
  ifsc_code VARCHAR(50),
  
  notes TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  
  FOREIGN KEY (store_id) REFERENCES store(id),
  UNIQUE(store_id, payment_type)
);
```

## Test Credentials

### eSewa Test
- **Merchant ID**: EPAYTEST
- **Secret Key**: 8gBm/:&EnhH.1/q
- **Test URL**: https://uat.esewa.com.np/epay/main

### Khalti Test
- **Public Key**: test_public_key_dc74e0fd57cb46cd93832aee0a507256
- **Secret Key**: test_secret_key_f59e8b7d18b4499ca40f68195a846e9b
- **Test Mobile**: 9800000000
- **Test OTP**: 987654

## Security Considerations

1. **Credentials Storage**: Payment gateway credentials are stored encrypted in database
2. **Access Control**: Only store admins can configure payment methods
3. **API Keys**: Never expose secret keys in frontend/logs
4. **Verification**: Always verify payment with gateway before completing order
5. **PCI Compliance**: Card payments should use tokenization (never store card details)

## Common Workflows

### Scenario 1: New Store Setup
1. Admin approves store registration
2. System initializes payment methods (CASH enabled)
3. Store admin logs in
4. Store admin configures eSewa and Khalti
5. Cashiers can now accept multiple payment types

### Scenario 2: Disable Payment Method
1. Store admin notices eSewa issues
2. Admin disables eSewa via toggle endpoint
3. eSewa no longer shown to cashiers
4. Fix configuration, re-enable when ready

### Scenario 3: Change Payment Gateway
1. Store admin updates eSewa credentials
2. Tests payment with small transaction
3. Verifies successful processing
4. Payment method ready for production use

## Troubleshooting

### Payment Method Not Showing
- Check if method is enabled: `GET /api/payment-config/store/enabled`
- Verify store has configuration
- Ensure credentials are valid

### Payment Verification Failed
- Check payment gateway credentials
- Verify network connectivity to gateway
- Check transaction ID format
- Review gateway response logs

### Cannot Configure Payment Method
- Ensure user is store admin
- Check if method already configured
- Verify all required fields provided

## Future Enhancements

- QR code payments
- Buy now, pay later (BNPL)
- Cryptocurrency payments
- Multi-currency support
- Payment analytics dashboard
- Automatic reconciliation
- Refund processing