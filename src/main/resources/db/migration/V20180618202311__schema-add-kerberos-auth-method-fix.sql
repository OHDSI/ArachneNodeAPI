ALTER TABLE datasource DROP COLUMN IF EXISTS krb_auth_method;

ALTER TABLE datasource
    ADD COLUMN IF NOT EXISTS krb_auth_method VARCHAR DEFAULT 'PASSWORD';

UPDATE datasource SET krb_auth_method = 'PASSWORD' WHERE
  krb_auth_method IS NULL
  AND use_kerberos = TRUE;