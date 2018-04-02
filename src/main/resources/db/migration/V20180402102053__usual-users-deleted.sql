DELETE FROM users u WHERE NOT EXISTS (SELECT 1 FROM users_roles ur WHERE ur.user_id = u.id);
DELETE FROM roles r WHERE r.name = 'ROLE_USER';