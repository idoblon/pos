V3__add_tax_and_unit_price.sql

-- Add tax tracking to orders
ALTER TABLE orders
  ADD COLUMN tax_amount DOUBLE DEFAULT 0.0;

-- Persist per-item unit price (currently only line-total "price" exists)
ALTER TABLE order_items
  ADD COLUMN unit_price DOUBLE;

-- Backfill unit_price from existing price/quantity so old orders keep correct display
UPDATE order_items oi
SET unit_price = oi.price / NULLIF(oi.quantity, 0)
WHERE unit_price IS NULL;

-- Fix existing orders: currently total_amount stores subtotal (without tax)
-- We need to compute tax = subtotal * 0.13, then update total_amount = subtotal + tax
UPDATE orders
SET tax_amount = total_amount * 0.13,
    total_amount = total_amount * 1.13
WHERE tax_amount IS NULL OR tax_amount = 0;
