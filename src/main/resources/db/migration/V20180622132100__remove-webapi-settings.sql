DELETE FROM system_settings USING system_settings_groups sg
WHERE group_id = sg.id AND sg.name = 'atlas';

DELETE FROM system_settings_groups WHERE name = 'atlas';