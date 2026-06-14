-- Check exact store admin data
SELECT id, full_name, email, role, store_id, branch_id FROM pos.user WHERE role = 'ROLE_STORE_ADMIN';

-- If store_id is NULL, fix it:
UPDATE pos.user SET store_id = 6 WHERE role = 'ROLE_STORE_ADMIN' AND store_id IS NULL;
