# ArachneNodeAPI
Arachne Data Node is a component that facilitates connection and communication between local databases and Arachne Central

## Running with Authenticator in a standalone mode

- Create database to store users (or use another authentication provided by Authenticator)
- Create users table and populate it (or use database from the ArachneCentralAPI)
- Run application applying the following parameters:

```
-Dauthenticator.methods.db.service=org.ohdsi.authenticator.service.jdbc.JdbcAuthService
-Dauthenticator.methods.db.config.jdbcUrl=jdbc:postgresql://localhost:5432/arachne_portal
-Dauthenticator.methods.db.config.username=user
-Dauthenticator.methods.db.config.password=secret
-Dauthenticator.methods.db.config.query="select password, id, firstname, lastname, email from users_data where email=:username"
-Dauthenticator.methods.db.config.passwordEncoder=org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
-Dauthenticator.methods.db.config.fieldsToExtract.firstName=firstname
-Dauthenticator.methods.db.config.fieldsToExtract.lastName=lastname
-Dauthenticator.methods.db.config.fieldsToExtract.id=T(com.odysseusinc.arachne.commons.utils.UserIdUtils).idToUuid(T(java.lang.Long).valueOf(id))
-Dauthenticator.methods.db.config.fieldsToExtract.email=email
-Dsecurity.method=db
```