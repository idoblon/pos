# Backend Subscription System Implementation

## Overview
Complete backend implementation for subscription management system with yearly subscriptions, automatic status updates, and notification system.

## Files Created

### 1. Entity Models

#### `SubscriptionNotification.java`
- Location: `src/main/java/com/springboot/POS/modal/SubscriptionNotification.java`
- Stores subscription notifications for stores
- Fields: id, store, type, title, message, priority, isRead, createdAt, expiresAt, daysRemaining

### 2. Repositories

#### `SubscriptionNotificationRepository.java`
- Location: `src/main/java/com/springboot/POS/repository/SubscriptionNotificationRepository.java`
- JPA repository for notification operations
- Custom queries for active notifications and unread counts

### 3. DTOs

#### `SubscriptionDTO.java`
- Subscription data transfer object
- Contains: storeId, storeName, plan, dates, status, pricing

#### `SubscriptionStatsDTO.java`
- Admin statistics DTO
- Contains: counts by status, revenue, plan distribution

#### `SubscriptionNotificationDTO.java`
- Notification data transfer object

#### `SubscriptionRequests.java`
- Request objects: SubscriptionRenewalRequest, SubscriptionPlanUpdateRequest, etc.

### 4. Service Layer

#### `SubscriptionService.java` (Interface)
- Service contract with all subscription operations

#### `SubscriptionServiceImpl.java` (Implementation)
- Complete business logic implementation
- Scheduled tasks for status updates and notifications
- Price calculation and validation

### 5. Controller

#### `SubscriptionController.java`
- REST API endpoints for subscription management
- Security with @PreAuthorize annotations
- Endpoints for stores and admins

## Database Changes

### Store Table Updates
```sql
ALTER TABLE store 
ADD COLUMN subscription_purchase_date TIMESTAMP,
ADD COLUMN subscription_expiry TIMESTAMP,
ADD COLUMN subscription_status VARCHAR(50) DEFAULT 'ACTIVE',
ADD COLUMN subscription_renewal_count INTEGER DEFAULT 0,
ADD COLUMN last_subscription_renewal TIMESTAMP;
```

### New Table: subscription_notifications
```sql
CREATE TABLE subscription_notifications (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP,
    days_remaining INTEGER,
    FOREIGN KEY (store_id) REFERENCES store(id) ON DELETE CASCADE
);
```

## API Endpoints

### Store Endpoints

#### Get Store Subscription
```
GET /api/stores/{storeId}/subscription
Authorization: ROLE_ADMIN, ROLE_STORE_ADMIN
Response: SubscriptionDTO
```

#### Get Current Store's Subscription
```
GET /api/store/subscription/current
Authorization: ROLE_STORE_ADMIN
Response: SubscriptionDTO
```

#### Create Subscription
```
POST /api/stores/{storeId}/subscription
Authorization: ROLE_ADMIN
Body: { plan, paymentDetails }
Response: SubscriptionDTO
```

#### Renew Subscription
```
POST /api/stores/{storeId}/subscription/renew
Authorization: ROLE_ADMIN, ROLE_STORE_ADMIN
Body: { plan, paymentDetails }
Response: SubscriptionDTO
```

#### Update Subscription Plan
```
PUT /api/stores/{storeId}/subscription/plan
Authorization: ROLE_ADMIN, ROLE_STORE_ADMIN
Body: { plan }
Response: SubscriptionDTO
```

#### Suspend Subscription
```
PATCH /api/stores/{storeId}/subscription/suspend
Authorization: ROLE_ADMIN
Body: { reason }
Response: ApiResponse
```

#### Reactivate Subscription
```
PATCH /api/stores/{storeId}/subscription/reactivate
Authorization: ROLE_ADMIN
Response: ApiResponse
```

### Admin Endpoints

#### Get Expiring Subscriptions
```
GET /api/admin/subscriptions/expiring?days=60
Authorization: ROLE_ADMIN
Response: List<SubscriptionDTO>
```

#### Get Subscription Statistics
```
GET /api/admin/subscriptions/stats
Authorization: ROLE_ADMIN
Response: SubscriptionStatsDTO
```

#### Manually Update Subscription Statuses
```
POST /api/admin/subscriptions/update-statuses
Authorization: ROLE_ADMIN
Response: ApiResponse
```

#### Manually Generate Notifications
```
POST /api/admin/subscriptions/generate-notifications
Authorization: ROLE_ADMIN
Response: ApiResponse
```

### Notification Endpoints

#### Get Store Notifications
```
GET /api/stores/{storeId}/subscription/notifications
Authorization: ROLE_ADMIN, ROLE_STORE_ADMIN
Response: List<SubscriptionNotificationDTO>
```

#### Mark Notification as Read
```
PATCH /api/subscription/notifications/{notificationId}/read
Authorization: ROLE_ADMIN, ROLE_STORE_ADMIN
Response: ApiResponse
```

## Subscription Plans & Pricing

```java
BASIC:        NPR 3,500/year  (NPR 292/month)
PROFESSIONAL: NPR 7,000/year  (NPR 583/month)
ENTERPRISE:   NPR 10,000/year (NPR 833/month)
```

## Subscription Status Flow

### Status Values
- **ACTIVE**: More than 30 days remaining
- **EXPIRING_SOON**: 1-30 days remaining
- **EXPIRED**: Expiry date passed
- **SUSPENDED**: Manually suspended by admin

### Automatic Status Updates
- Runs daily at 2 AM via scheduled task
- Updates all store subscription statuses
- Suspends stores with expired subscriptions

## Notification System

### Notification Types
- **EXPIRED**: Subscription has expired (High priority)
- **CRITICAL**: Expiring in 7 days or less (High priority)
- **WARNING**: Expiring in 30 days or less (Medium priority)

### Notification Generation
- Runs daily at 8 AM via scheduled task
- Generates notifications at key intervals: 60, 30, 7, and 0 days
- Prevents duplicate notifications
- Cleans up old read notifications

## Scheduled Tasks

### Update Subscription Statuses
```java
@Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
public void updateSubscriptionStatuses()
```

### Generate Expiration Notifications
```java
@Scheduled(cron = "0 0 8 * * *") // Daily at 8 AM
public void generateExpirationNotifications()
```

## Business Logic

### Subscription Creation
1. Set purchase date to current date
2. Calculate expiry date (purchase + 1 year)
3. Set status to ACTIVE
4. Initialize renewal count to 0
5. Activate store if suspended

### Subscription Renewal
1. Update purchase date to current date
2. Calculate new expiry date (now + 1 year)
3. Reset status to ACTIVE
4. Increment renewal count
5. Record last renewal date
6. Clear old notifications
7. Reactivate store if suspended

### Status Calculation Logic
```java
if (expiryDate < now) -> EXPIRED
if (daysRemaining <= 30) -> EXPIRING_SOON
else -> ACTIVE
```

### Suspension Logic
- Sets subscription status to SUSPENDED
- Sets store status to SUSPENDED
- Creates high-priority notification
- Prevents store from operating

## Migration Steps

### 1. Run SQL Migration
```bash
psql -U your_user -d your_database -f SUBSCRIPTION_MIGRATION.sql
```

### 2. Restart Application
```bash
mvn spring-boot:run
```

### 3. Verify Tables
```sql
SELECT * FROM store LIMIT 5;
SELECT * FROM subscription_notifications LIMIT 5;
```

### 4. Test Endpoints
```bash
# Get subscription stats
curl -X GET http://localhost:8080/api/admin/subscriptions/stats \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Get current subscription
curl -X GET http://localhost:8080/api/store/subscription/current \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Integration Points

### With Existing Systems

#### Store Registration
- On approval, set subscription purchase and expiry dates
- Initialize subscription status as ACTIVE

#### Payment System
- On successful payment, call renewal endpoint
- Update subscription dates automatically

#### Store Access Control
- Check subscription status before allowing operations
- Redirect expired stores to renewal page

## Testing Checklist

### Backend Testing
- [ ] Store model updates correctly
- [ ] Subscription creation works
- [ ] Renewal updates dates correctly
- [ ] Status calculation accurate
- [ ] Notifications generated at correct intervals
- [ ] Scheduled tasks run properly
- [ ] API endpoints return correct data
- [ ] Security annotations working
- [ ] Database constraints working

### Integration Testing
- [ ] Frontend can fetch subscription data
- [ ] Notifications display correctly
- [ ] Renewal flow works end-to-end
- [ ] Admin can view expiring subscriptions
- [ ] Store access blocked when expired
- [ ] Email notifications sent (if implemented)

## Error Handling

### Common Errors
- Store not found: Return 404 with message
- Invalid plan: Return 400 with validation message
- Expired subscription: Return 403 with renewal prompt
- Unauthorized access: Return 401

### Exception Handling
All exceptions are handled by `GlobalExceptionHandler.java`

## Performance Considerations

### Database Indexes
- Created on subscription_expiry for faster queries
- Created on subscription_status for filtering
- Created on notification store_id and is_read

### Query Optimization
- Use @Transactional(readOnly = true) for read operations
- Batch update subscription statuses
- Limit notification queries with pagination

## Security

### Role-Based Access
- **ADMIN**: Full access to all subscription operations
- **STORE_ADMIN**: Can view and renew own subscription
- **Other roles**: No subscription access

### JWT Validation
- All endpoints require valid JWT token
- Email extracted from JWT for current user operations

## Monitoring

### Metrics to Track
- Daily subscription renewals
- Expiring subscriptions count
- Notification generation success rate
- Failed renewal attempts
- Revenue by plan type

### Logs to Monitor
- Scheduled task execution
- Subscription status changes
- Notification generation
- API endpoint calls

## Future Enhancements

### Potential Features
1. Email notifications for expiring subscriptions
2. Automatic payment processing
3. Subscription plan downgrades
4. Proration for mid-cycle upgrades
5. Multi-year subscription options
6. Grace period before suspension
7. Subscription history tracking
8. Analytics dashboard for admins
9. Webhook integration for third-party systems
10. Automated reminder emails

## Troubleshooting

### Issue: Scheduled tasks not running
**Solution**: Ensure @EnableScheduling is on main application class

### Issue: Subscription status not updating
**Solution**: Check scheduled task logs and database connection

### Issue: Notifications duplicating
**Solution**: Check notification existence logic in generateExpirationNotifications

### Issue: Store can't renew subscription
**Solution**: Verify JWT token and ROLE_STORE_ADMIN permission

## Contact & Support

For issues or questions about subscription system:
1. Check logs in application console
2. Verify database state with SQL queries
3. Test API endpoints with Postman
4. Review frontend integration in SUBSCRIPTION_SYSTEM.md
