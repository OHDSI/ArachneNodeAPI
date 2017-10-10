/**
 *
 * Copyright 2017 Observational Health Data Sciences and Informatics
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: October 02, 2017
 *
 */

CREATE TABLE IF NOT EXISTS users
(
  id         BIGSERIAL PRIMARY KEY,
  first_name VARCHAR(50),
  last_name  VARCHAR(50),
  email      VARCHAR(100)
    CONSTRAINT user_email_key
    UNIQUE,
  token      VARCHAR,
  username   VARCHAR NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS users_id_seq MINVALUE 1;
ALTER SEQUENCE users_id_seq OWNED BY users.id;
SELECT setval('users_id_seq', (SELECT MAX(id) + 1
                               FROM users), FALSE);

CREATE TABLE IF NOT EXISTS roles
(
  name VARCHAR(50) NOT NULL,
  id   BIGSERIAL PRIMARY KEY
);

ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_uk;
ALTER TABLE roles ADD CONSTRAINT roles_name_uk UNIQUE (name);

CREATE SEQUENCE IF NOT EXISTS roles_id_seq MINVALUE 1;
ALTER SEQUENCE roles_id_seq OWNED BY roles.id;
SELECT setval('roles_id_seq', (SELECT MAX(id) + 1
                               FROM roles), FALSE);

CREATE TABLE IF NOT EXISTS users_roles
(
  user_id BIGINT NOT NULL
    REFERENCES users
    ON UPDATE CASCADE ON DELETE CASCADE,
  role_id BIGINT NOT NULL
    REFERENCES roles
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT users_roles_user_id_role_id_pk
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS token_blacklist
(
  id                   TEXT NOT NULL
    PRIMARY KEY,
  blacklistedtimestamp TIMESTAMP,
  expirationtimestamp  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS studies
(
  id    BIGSERIAL
    PRIMARY KEY,
  title VARCHAR(1024) NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS studies_id_seq MINVALUE 1;
ALTER SEQUENCE studies_id_seq OWNED BY studies.id;
SELECT setval('studies_id_seq', (SELECT MAX(id) + 1
                                 FROM studies), FALSE);

CREATE TABLE IF NOT EXISTS datanode
(
  id                        BIGSERIAL
    PRIMARY KEY,
  sid                       VARCHAR(50)                                          NOT NULL
    CONSTRAINT datanode_sid_key
    UNIQUE,
  health_status             VARCHAR DEFAULT 'NOT_COLLECTED' :: CHARACTER VARYING NOT NULL,
  health_status_description VARCHAR,
  name                      VARCHAR                                              NOT NULL,
  description               VARCHAR                                              NOT NULL,
  token                     VARCHAR                                              NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS datanode_id_seq MINVALUE 1;
ALTER SEQUENCE datanode_id_seq OWNED BY datanode.id;
SELECT setval('datanode_id_seq', (SELECT MAX(id) + 1
                                  FROM datanode), FALSE);

CREATE TABLE IF NOT EXISTS datasource
(
  id                        BIGSERIAL PRIMARY KEY,
  sid                       VARCHAR(50)                                          NOT NULL
    CONSTRAINT nodedatasource_sid_key
    UNIQUE,
  name                      VARCHAR                                              NOT NULL,
  description               VARCHAR,
  cdm_schema                VARCHAR                                              NOT NULL,
  dbms_host                 VARCHAR,
  dbms_username             VARCHAR,
  dbms_password             VARCHAR,
  dbms_type                 VARCHAR,
  dbms_version              VARCHAR,
  dbms_port                 INTEGER,
  db_name                   VARCHAR,
  data_node_id              BIGINT,
  registred                 BOOLEAN DEFAULT FALSE                                NOT NULL,
  domain                    VARCHAR(128),
  parameters                VARCHAR,
  connection_string         VARCHAR,
  model_type                VARCHAR                                              NOT NULL,
  health_status             VARCHAR DEFAULT 'NOT_COLLECTED' :: CHARACTER VARYING NOT NULL,
  health_status_description VARCHAR
);

CREATE SEQUENCE IF NOT EXISTS datasource_id_seq MINVALUE 1;
ALTER SEQUENCE datasource_id_seq OWNED BY datasource.id;
SELECT setval('datasource_id_seq', (SELECT MAX(id) + 1
                                    FROM datasource), FALSE);

CREATE TABLE IF NOT EXISTS system_settings_groups
(
  id    BIGSERIAL
    PRIMARY KEY,
  label VARCHAR(255) NOT NULL,
  name  VARCHAR(255) NOT NULL
    CONSTRAINT name_uk
    UNIQUE
);

CREATE SEQUENCE IF NOT EXISTS system_settings_groups_id_seq MINVALUE 1;
ALTER SEQUENCE system_settings_groups_id_seq OWNED BY system_settings_groups.id;
SELECT setval('system_settings_groups_id_seq', (SELECT MAX(id) + 1
                                                FROM system_settings_groups), FALSE);

CREATE TABLE IF NOT EXISTS system_settings
(
  id       BIGSERIAL
    PRIMARY KEY,
  group_id BIGINT       NOT NULL
    REFERENCES system_settings_groups
    ON UPDATE CASCADE ON DELETE CASCADE,
  label    VARCHAR(255) NOT NULL,
  name     VARCHAR(255) NOT NULL,
  value    VARCHAR(1024),
  type     VARCHAR DEFAULT 'text' :: CHARACTER VARYING
);

ALTER TABLE system_settings DROP CONSTRAINT IF EXISTS system_settings_name_unique;
ALTER TABLE system_settings ADD CONSTRAINT system_settings_name_unique UNIQUE (name);

CREATE SEQUENCE IF NOT EXISTS system_settings_id_seq MINVALUE 1;
ALTER SEQUENCE system_settings_id_seq OWNED BY system_settings.id;
SELECT setval('system_settings_id_seq', (SELECT MAX(id) + 1
                                         FROM system_settings), FALSE);

CREATE TABLE IF NOT EXISTS achilles_jobs
(
  id            BIGSERIAL
    PRIMARY KEY,
  datasource_id BIGINT                                 NOT NULL
    REFERENCES datasource
    ON UPDATE CASCADE ON DELETE CASCADE,
  started       TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
  status        VARCHAR                                NOT NULL,
  finished      TIMESTAMP WITH TIME ZONE,
  achilles_log  TEXT,
  source        VARCHAR
);

CREATE SEQUENCE IF NOT EXISTS achilles_jobs_id_seq MINVALUE 1;
ALTER SEQUENCE achilles_jobs_id_seq OWNED BY achilles_jobs.id;
SELECT setval('achilles_jobs_id_seq', (SELECT MAX(id) + 1
                                       FROM achilles_jobs), FALSE);

CREATE TABLE IF NOT EXISTS common_entity
(
  id            BIGSERIAL
    PRIMARY KEY,
  guid          VARCHAR NOT NULL
    CONSTRAINT common_entity_guid_key
    UNIQUE,
  analysis_type VARCHAR NOT NULL,
  local_id      INTEGER NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS common_entity_id_seq MINVALUE 1;
ALTER SEQUENCE common_entity_id_seq OWNED BY common_entity.id;
SELECT setval('common_entity_id_seq', (SELECT MAX(id) + 1
                                       FROM common_entity), FALSE);

INSERT INTO system_settings_groups (label, name) VALUES ('Integration', 'integration') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings_groups (label, name) VALUES ('Proxy', 'proxy') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings_groups (label, name) VALUES ('Achilles', 'achilles') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings_groups (label, name) VALUES ('Atlas WebAPI', 'atlas') ON CONFLICT (name) DO NOTHING;

INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'integration'), 'Datanode (this) url', 'datanode.baseURL', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'integration'), 'Datanode (this) port', 'datanode.port', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'integration'), 'Central host', 'datanode.arachneCentral.host', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'integration'), 'Central port', 'datanode.arachneCentral.port', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'integration'), 'Engine protocol', 'executionEngine.protocol', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'integration'), 'Engine host', 'executionEngine.host', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'integration'), 'Engine port', 'executionEngine.port', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'integration'), 'Engine analysisUri', 'executionEngine.analysisUri', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'integration'), 'Engine token', 'executionEngine.token', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'proxy'), 'Proxy host', 'proxy.host', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'proxy'), 'Proxy port', 'proxy.port', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'proxy'), 'Proxy username', 'proxy.auth.username', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'proxy'), 'Proxy password', 'proxy.auth.password', null, 'password') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'achilles'), 'Cron', 'achilles.scheduler.cron', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'atlas'), 'Host', 'atlas.host', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'atlas'), 'Port', 'atlas.port', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'atlas'), 'Check interval', 'atlas.scheduler.checkInterval', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'atlas'), 'Target DB schema', 'cohorts.target.dbSchema', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'atlas'), 'Result DB schema', 'cohorts.result.dbSchema', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'atlas'), 'Target Cohort table', 'cohorts.target.cohortTable', null, 'text') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'proxy'), 'Proxy enabled', 'proxy.enabled', null, 'checkbox') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'proxy'), 'Proxy auth enabled', 'proxy.auth.enabled', null, 'checkbox') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'achilles'), 'Scheduler enabled', 'achilles.scheduler.enabled', null, 'checkbox') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'atlas'), 'Cohort patients log enabled', 'cohorts.result.summaryEnabled', null, 'checkbox') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'atlas'), 'Cohort patients count enable', 'cohorts.result.countEnabled', null, 'checkbox') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'integration'), 'SSL enabled', 'server.ssl.enabled', null, 'checkbox') ON CONFLICT (name) DO NOTHING;
INSERT INTO system_settings (group_id, label, name, value, type) VALUES ((SELECT id FROM system_settings_groups WHERE name = 'integration'), 'SSL strict mode enabled', 'server.ssl.strictMode', null, 'checkbox') ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_USER') ON CONFLICT (name) DO NOTHING;