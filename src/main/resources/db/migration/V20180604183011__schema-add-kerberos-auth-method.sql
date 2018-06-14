ALTER TABLE datasource
    ADD COLUMN IF NOT EXISTS krb_auth_method VARCHAR DEFAULT 'password';

UPDATE datasource SET krb_auth_method = 'password' WHERE
  krb_auth_method IS NULL
  AND use_kerberos = TRUE;