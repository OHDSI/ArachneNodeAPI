ALTER TABLE datasource
  ADD use_kerberos BOOLEAN DEFAULT FALSE NOT NULL,
  ADD krb_realm VARCHAR,
  ADD krb_fqdn VARCHAR,
  ADD krb_user VARCHAR,
  ADD krb_password VARCHAR,
  ADD krb_keytab BYTEA;