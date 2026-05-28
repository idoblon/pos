-- Change image column to TEXT to store base64 encoded images
ALTER TABLE product MODIFY COLUMN image TEXT;

-- Verify the change
DESCRIBE product;
