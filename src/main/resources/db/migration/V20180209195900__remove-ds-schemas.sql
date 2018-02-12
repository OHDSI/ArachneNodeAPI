ALTER TABLE datanode ALTER name DROP NOT NULL;
ALTER TABLE datanode ALTER description DROP NOT NULL;

ALTER TABLE datasource DROP COLUMN registred;