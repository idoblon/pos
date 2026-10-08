-- Vertical store-type attributes for products, orders and order items.
-- Dormant until Flyway is enabled (ddl-auto=update manages the schema today);
-- kept in sync with the JPA entities so a future Flyway baseline stays aligned.

-- ── Product vertical attributes ──
ALTER TABLE product ADD COLUMN IF NOT EXISTS expiry_date DATE;
ALTER TABLE product ADD COLUMN IF NOT EXISTS batch_number VARCHAR(64);
ALTER TABLE product ADD COLUMN IF NOT EXISTS prescription_required BOOLEAN DEFAULT FALSE;
ALTER TABLE product ADD COLUMN IF NOT EXISTS controlled_substance BOOLEAN DEFAULT FALSE;
ALTER TABLE product ADD COLUMN IF NOT EXISTS dosage VARCHAR(255);
ALTER TABLE product ADD COLUMN IF NOT EXISTS unit VARCHAR(16);
ALTER TABLE product ADD COLUMN IF NOT EXISTS weight DECIMAL(19,3);
ALTER TABLE product ADD COLUMN IF NOT EXISTS weight_step DECIMAL(19,3);
ALTER TABLE product ADD COLUMN IF NOT EXISTS moq INT;
ALTER TABLE product ADD COLUMN IF NOT EXISTS requires_serial BOOLEAN DEFAULT FALSE;
ALTER TABLE product ADD COLUMN IF NOT EXISTS warranty_months INT;
ALTER TABLE product ADD COLUMN IF NOT EXISTS size_variant VARCHAR(64);
ALTER TABLE product ADD COLUMN IF NOT EXISTS color_variant VARCHAR(64);
ALTER TABLE product ADD COLUMN IF NOT EXISTS variants_json LONGTEXT;
ALTER TABLE product ADD COLUMN IF NOT EXISTS bulk_min_qty INT;
ALTER TABLE product ADD COLUMN IF NOT EXISTS bulk_price DECIMAL(19,2);
ALTER TABLE product ADD COLUMN IF NOT EXISTS bulk_tiers_json LONGTEXT;
ALTER TABLE product ADD COLUMN IF NOT EXISTS preparation_time INT;
ALTER TABLE product ADD COLUMN IF NOT EXISTS kitchen_station VARCHAR(64);
ALTER TABLE product ADD COLUMN IF NOT EXISTS modifiers_json LONGTEXT;
ALTER TABLE product ADD COLUMN IF NOT EXISTS is_veg BOOLEAN DEFAULT FALSE;
ALTER TABLE product ADD COLUMN IF NOT EXISTS care_instructions VARCHAR(512);
ALTER TABLE product ADD COLUMN IF NOT EXISTS guarantee_days INT;

-- ── Order vertical context ──
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_type VARCHAR(16);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS table_number VARCHAR(32);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS kitchen_note VARCHAR(500);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS prescription_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS emi_months INT;

-- ── Order-item detail + fractional (weighted) quantities ──
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS modifiers_json LONGTEXT;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS kitchen_note VARCHAR(255);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS dosage VARCHAR(255);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS serials_json LONGTEXT;
-- Fractional (weighted) quantities, e.g. 0.5 kg. Existing integer values convert cleanly.
ALTER TABLE order_items MODIFY COLUMN quantity DECIMAL(19,3);
