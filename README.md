# ArachneNodeAPI
Arachne Data Node is a component that facilitates connection and communication between local databases and Arachne Central

## Running with Authenticator in a standalone mode

- Create database to store users (or use another authentication provided by Authenticator)
- Create users table and populate it (or use database from the ArachneCentralAPI)
- Apply create if not exists user registration strategy by (see about strategies below)
```
-Dauthenticator.user.registrationStrategy=CREATE_IF_NOT_EXISTS
```
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

## User registration strategies

There are two user registration strategies:
- **CREATE_IF_FIRST** - means first user succcesfully logged in registered by system 
and became Data Node administrator. Other users should be added by administrator manually
using Admin section.
- **CREATE_IF_NOT_EXISTS** - means any user who logged in would be registered if one's not
registered before. This useful when using external authentication source like Active Directory.
When external authentication source is used then Admin page could not be used to add users
since don't know where to find users.  