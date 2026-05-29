-- Drop the misspelled column and keep the correct one
ALTER TABLE product DROP COLUMN desciption;

-- Verify the change
DESCRIBE product;
