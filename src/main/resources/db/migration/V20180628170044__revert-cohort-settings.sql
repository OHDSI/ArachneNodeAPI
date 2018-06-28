INSERT INTO system_settings_groups (label, name) VALUES ('Atlas WebAPI', 'atlas') ON CONFLICT (name) DO NOTHING;

INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'atlas'), 'Cohort patients log enabled', 'cohorts.result.summaryEnabled', null, 'checkbox') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'atlas'), 'Cohort patients count enable', 'cohorts.result.countEnabled', null, 'checkbox') ON CONFLICT (name) DO NOTHING;