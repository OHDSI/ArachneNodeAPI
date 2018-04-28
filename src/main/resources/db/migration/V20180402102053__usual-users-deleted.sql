DELETE FROM users u WHERE u.id IN (
  SELECT ur.user_id FROM users_roles ur WHERE ur.role_id = (
    SELECT r.id FROM roles r WHERE r.name = 'ROLE_USER'
  )
);

DELETE FROM roles r WHERE r.name = 'ROLE_USER';

DELETE FROM users_roles;

INSERT INTO users_roles (user_id, role_id)
  SELECT u.id, (SELECT r.id FROM roles r WHERE r.name = 'ROLE_ADMIN') FROM users u;