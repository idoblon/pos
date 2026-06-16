-- Subscription System Database Migration
-- Add subscription tracking columns to Store table

-- Add subscription tracking columns
ALTER TABLE store 
ADD COLUMN IF NOT EXISTS subscription_purchase_date TIMESTAMP,
ADD COLUMN IF NOT EXISTS subscription_expiry TIMESTAMP,
ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(50) DEFAULT 'ACTIVE',
ADD COLUMN IF NOT EXISTS subscription_renewal_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS last_subscription_renewal TIMESTAMP;

-- Update existing stores with initial subscription data
UPDATE store 
SET 
    subscription_purchase_date = COALESCE(approved_at, created_at, NOW()),
    subscription_expiry = COALESCE(approved_at, created_at, NOW()) + INTERVAL '1 year',
    subscription_status = 'ACTIVE',
    subscription_renewal_count = 0
WHERE subscription_purchase_date IS NULL;

-- Create subscription_notifications table
CREATE TABLE IF NOT EXISTS subscription_notifications (
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

-- Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_subscription_expiry ON store(subscription_expiry);
CREATE INDEX IF NOT EXISTS idx_subscription_status ON store(subscription_status);
CREATE INDEX IF NOT EXISTS idx_notification_store ON subscription_notifications(store_id);
CREATE INDEX IF NOT EXISTS idx_notification_is_read ON subscription_notifications(is_read);

-- Update subscription statuses based on expiry dates
UPDATE store 
SET subscription_status = 
    CASE 
        WHEN subscription_expiry < NOW() THEN 'EXPIRED'
        WHEN subscription_expiry <= NOW() + INTERVAL '30 days' THEN 'EXPIRING_SOON'
        ELSE 'ACTIVE'
    END
WHERE subscription_expiry IS NOT NULL;

-- Generate initial notifications for expiring subscriptions
INSERT INTO subscription_notifications (store_id, type, title, message, priority, days_remaining)
SELECT 
    id,
    CASE 
        WHEN subscription_expiry < NOW() THEN 'EXPIRED'
        WHEN EXTRACT(DAY FROM (subscription_expiry - NOW())) <= 7 THEN 'CRITICAL'
        ELSE 'WARNING'
    END as type,
    CASE 
        WHEN subscription_expiry < NOW() THEN 'Subscription Expired'
        ELSE 'Subscription Expiring Soon'
    END as title,
    CASE 
        WHEN subscription_expiry < NOW() THEN 'Your subscription has expired. Renew now to continue using the POS system.'
        ELSE 'Your subscription expires in ' || EXTRACT(DAY FROM (subscription_expiry - NOW())) || ' days. Renew now to avoid service interruption.'
    END as message,
    CASE 
        WHEN subscription_expiry < NOW() OR EXTRACT(DAY FROM (subscription_expiry - NOW())) <= 7 THEN 'HIGH'
        ELSE 'MEDIUM'
    END as priority,
    CAST(EXTRACT(DAY FROM (subscription_expiry - NOW())) AS INTEGER) as days_remaining
FROM store
WHERE subscription_expiry IS NOT NULL 
  AND subscription_expiry <= NOW() + INTERVAL '60 days'
  AND NOT EXISTS (
      SELECT 1 FROM subscription_notifications sn 
      WHERE sn.store_id = store.id 
      AND sn.is_read = FALSE
  );

COMMIT;
