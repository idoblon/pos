-- Add store_id column to customer table (nullable first to handle existing rows)
ALTER TABLE customer ADD COLUMN IF NOT EXISTS store_id BIGINT;

-- Assign existing customers to the first store to avoid orphaned data
-- (existing customers before this fix had no store scope)
UPDATE customer SET store_id = (SELECT MIN(id) FROM store) WHERE store_id IS NULL;

-- Now enforce NOT NULL
ALTER TABLE customer ALTER COLUMN store_id SET NOT NULL;

-- Add foreign key constraint
ALTER TABLE customer ADD CONSTRAINT fk_customer_store
    FOREIGN KEY (store_id) REFERENCES store(id);

-- Index for fast store-scoped lookups
CREATE INDEX IF NOT EXISTS idx_customer_store_id ON customer(store_id);
