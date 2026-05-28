-- Rename misspelled column in product table
ALTER TABLE product CHANGE COLUMN desciption description VARCHAR(255);

-- Verify the change
DESCRIBE product;
