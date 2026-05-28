-- Drop both description columns from product table
ALTER TABLE product DROP COLUMN description;
ALTER TABLE product DROP COLUMN desciption;

-- Verify the change
DESCRIBE product;
