# Quick Setup Guide - Subscription System

## Prerequisites
- Backend running (Spring Boot)
- Frontend running (React + Vite)
- PostgreSQL database
- Admin access to database

## Backend Setup (5 minutes)

### Step 1: Run Database Migration
```bash
cd c:\Users\hp\IdeaProjects\POS
psql -U postgres -d your_database_name -f SUBSCRIPTION_MIGRATION.sql
```

Or connect to your database and run the SQL manually.

### Step 2: Restart Backend Application
```bash
mvn spring-boot:run
```

The application will automatically:
- Enable scheduling (@EnableScheduling)
- Create subscription entities
- Start scheduled tasks
- Initialize subscription endpoints

### Step 3: Verify Backend
```bash
# Check if server is running
curl http://localhost:8080/

# Test subscription stats endpoint (replace JWT_TOKEN)
curl -X GET http://localhost:8080/api/admin/subscriptions/stats \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Frontend Setup (Already Done!)

All frontend components are already created:
- ✅ Subscription utilities
- ✅ Subscription service
- ✅ Notification banner
- ✅ Current subscription card
- ✅ Store subscription page
- ✅ Admin dashboard integration

## Verify Integration

### 1. Test Admin Dashboard
1. Login as admin
2. Go to admin dashboard
3. Check "Expiring Subscriptions" card shows count
4. Click bell icon to see notifications

### 2. Test Store Admin View
1. Login as store admin
2. Navigate to subscription page
3. Should see current subscription card
4. Should see notification banner if expiring

### 3. Test API Endpoints

#### Get Current Subscription (Store Admin)
```javascript
// Frontend will call this automatically
GET /api/store/subscription/current
```

#### Get Expiring Subscriptions (Admin)
```javascript
GET /api/admin/subscriptions/expiring?days=60
```

#### Get Subscription Stats (Admin)
```javascript
GET /api/admin/subscriptions/stats
```

## Scheduled Tasks

Once backend is running, these will execute automatically:

### Daily at 2 AM - Update Statuses
- Updates all subscription statuses
- Suspends expired stores
- No manual intervention needed

### Daily at 8 AM - Generate Notifications
- Creates notifications for:
  - Expired subscriptions (0 days)
  - Critical (7 days)
  - Warning (30 days)
  - Advance notice (60 days)

## Testing Scenarios

### Scenario 1: New Store Registration
1. Store gets approved
2. Subscription automatically created
3. Purchase date = approval date
4. Expiry date = approval date + 1 year
5. Status = ACTIVE

### Scenario 2: Expiring Subscription
1. Store with 25 days remaining
2. Status automatically changes to EXPIRING_SOON
3. Notification generated
4. Store admin sees banner
5. Admin sees in expiring list

### Scenario 3: Renewal
1. Store admin clicks "Renew Now"
2. Proceeds to payment
3. On payment success, call renewal API
4. Dates updated, status reset to ACTIVE
5. Renewal count incremented

### Scenario 4: Expired Subscription
1. Subscription passes expiry date
2. Status changes to EXPIRED
3. Store status changes to SUSPENDED
4. Store can't access system
5. Must renew to reactivate

## Manual Testing Commands

### Check Store Subscriptions in Database
```sql
SELECT 
    id,
    brand,
    subscription_plan,
    subscription_purchase_date,
    subscription_expiry,
    subscription_status,
    EXTRACT(DAY FROM (subscription_expiry - NOW())) as days_remaining
FROM store
ORDER BY subscription_expiry;
```

### Check Notifications
```sql
SELECT 
    sn.id,
    s.brand as store_name,
    sn.type,
    sn.title,
    sn.days_remaining,
    sn.is_read,
    sn.created_at
FROM subscription_notifications sn
JOIN store s ON s.id = sn.store_id
ORDER BY sn.created_at DESC
LIMIT 10;
```

### Manually Trigger Status Update
```bash
curl -X POST http://localhost:8080/api/admin/subscriptions/update-statuses \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

### Manually Generate Notifications
```bash
curl -X POST http://localhost:8080/api/admin/subscriptions/generate-notifications \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

## Troubleshooting

### Issue: "Expiring Subscriptions" shows 0
**Check:**
```sql
-- Verify stores have expiry dates
SELECT COUNT(*) FROM store WHERE subscription_expiry IS NOT NULL;

-- Check if any are expiring soon
SELECT COUNT(*) FROM store 
WHERE subscription_expiry BETWEEN NOW() AND NOW() + INTERVAL '60 days';
```

**Fix:** Run migration script again if columns are missing.

### Issue: No notifications appearing
**Check:**
```sql
SELECT COUNT(*) FROM subscription_notifications;
```

**Fix:** Manually trigger notification generation:
```bash
curl -X POST http://localhost:8080/api/admin/subscriptions/generate-notifications \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

### Issue: Scheduled tasks not running
**Check:** Application logs for:
```
Scheduled task 'updateSubscriptionStatuses' completed
Scheduled task 'generateExpirationNotifications' completed
```

**Fix:** Ensure `@EnableScheduling` is in PosApplication.java

### Issue: Frontend can't fetch subscription data
**Check:** Browser console for API errors

**Fix:** Verify:
1. Backend is running (port 8080)
2. JWT token is valid
3. CORS is configured
4. API endpoints match

## Data Initialization

If you need to set up test data:

```sql
-- Set a store to expire in 25 days (for testing)
UPDATE store 
SET subscription_expiry = NOW() + INTERVAL '25 days',
    subscription_status = 'EXPIRING_SOON'
WHERE id = 1;

-- Set a store to expire in 7 days (critical)
UPDATE store 
SET subscription_expiry = NOW() + INTERVAL '7 days',
    subscription_status = 'EXPIRING_SOON'
WHERE id = 2;

-- Set a store as expired
UPDATE store 
SET subscription_expiry = NOW() - INTERVAL '5 days',
    subscription_status = 'EXPIRED'
WHERE id = 3;
```

Then trigger notification generation to see all notification types.

## Production Deployment

### Before Going Live:
1. ✅ Run database migration
2. ✅ Test all API endpoints
3. ✅ Verify scheduled tasks execute
4. ✅ Test frontend integration
5. ✅ Set up monitoring/alerts
6. ✅ Configure email notifications (future)
7. ✅ Test payment integration
8. ✅ Backup database

### Environment Variables (if needed)
```properties
# application.properties
subscription.expiry.warning.days=30
subscription.expiry.critical.days=7
subscription.expiry.advance.days=60
```

## Success Indicators

System is working correctly when:
- ✅ Admin dashboard shows expiring count
- ✅ Store admins see notification banners
- ✅ Subscription cards display dates correctly
- ✅ Scheduled tasks run daily
- ✅ Database has subscription data
- ✅ API endpoints return data
- ✅ Status updates automatically
- ✅ Notifications generate daily

## Next Steps

1. **Test the system** with real store data
2. **Monitor scheduled tasks** for first few days
3. **Gather feedback** from store admins
4. **Implement payment integration** for renewals
5. **Add email notifications** for reminders
6. **Create admin reports** for subscription analytics

## Support

For issues:
1. Check `BACKEND_SUBSCRIPTION_IMPLEMENTATION.md` for detailed docs
2. Check `SUBSCRIPTION_SYSTEM.md` for frontend docs
3. Review application logs
4. Test with Postman/curl
5. Verify database state with SQL queries

System is now ready to manage store subscriptions! 🎉
