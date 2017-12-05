INSERT INTO system_settings (group_id, label, name, value, type) VALUES((SELECT id FROM system_settings_groups WHERE name = 'atlas'), 'Authentication type', 'atlas.auth.schema', 'NONE', 'text');
INSERT INTO system_settings (group_id, label, name, value, type) VALUES((SELECT id FROM system_settings_groups WHERE name = 'atlas'), 'Username', 'atlas.auth.username', null, 'text');
INSERT INTO system_settings (group_id, label, name, value, type) VALUES((SELECT id FROM system_settings_groups WHERE name = 'atlas'), 'Password', 'atlas.auth.password', null, 'password');
