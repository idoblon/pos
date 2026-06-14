-- Check store admin user for store 6
SELECT id, full_name, email, role, store_id, branch_id 
FROM pos.user 
WHERE role = 'ROLE_STORE_ADMIN' AND store_id = 6;

-- Check all store admins
SELECT id, full_name, email, role, store_id, branch_id 
FROM pos.user 
WHERE role = 'ROLE_STORE_ADMIN';
