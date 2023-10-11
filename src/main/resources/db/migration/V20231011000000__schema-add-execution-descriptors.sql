CREATE SEQUENCE IF NOT EXISTS descriptor_id_seq MINVALUE 1;

CREATE TABLE environment_descriptor (
    id BIGSERIAL PRIMARY KEY,
    descriptor_id VARCHAR NOT NULL,
    json VARCHAR,
    label VARCHAR,
    base BOOLEAN
);
